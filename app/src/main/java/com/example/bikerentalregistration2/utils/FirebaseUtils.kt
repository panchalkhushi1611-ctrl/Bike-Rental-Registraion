package com.example.bikerentalregistration2.utils

import com.example.bikerentalregistration2.models.Customer
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

/**
 * ALL Firebase code lives in this one file.
 * Activities only call these functions and never touch Firebase directly.
 */
object FirebaseUtils {

    const val CUSTOMERS = "customers"
    const val ADMINS = "admins"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ---------------------------------------------------------------- AUTH

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Save the display name on the account, then report success
                val user = task.result?.user
                if (user != null) {
                    val update = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
                    user.updateProfile(update).addOnCompleteListener { onResult(true, null) }
                } else {
                    onResult(true, null)
                }
            } else {
                onResult(false, authErrorMessage(task.exception))
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(true, null)
            else onResult(false, authErrorMessage(task.exception))
        }
    }

    fun logout() {
        auth.signOut()
    }

    // ----------------------------------------------------------- CUSTOMERS

    /**
     * Saves the registration. The document ID is the user's UID, so one user can register only once.
     * onResult(success, customerId, error)
     */
    fun saveCustomer(
        fullName: String,
        phone: String,
        dob: String,
        email: String,
        address: String,
        bikeCategory: String,
        onResult: (success: Boolean, customerId: String?, error: String?) -> Unit
    ) {
        val user = currentUser()
        if (user == null) {
            onResult(false, null, "You are not logged in. Please log in again.")
            return
        }
        val ref = db.collection(CUSTOMERS).document(user.uid)

        // Read from the SERVER first: this fails when offline (so we never show a fake success)
        // and also stops a second registration for the same user.
        ref.get(Source.SERVER).addOnCompleteListener { check ->
            if (!check.isSuccessful) {
                onResult(false, null, firestoreErrorMessage(check.exception))
                return@addOnCompleteListener
            }
            if (check.result?.exists() == true) {
                onResult(false, null, "You have already registered.")
                return@addOnCompleteListener
            }

            val customerId = "BRS-" + (100000..999999).random()
            val data = hashMapOf<String, Any>(
                "uid" to user.uid,
                "customerId" to customerId,
                "fullName" to fullName,
                "phoneNumber" to phone,
                "dateOfBirth" to dob,
                "email" to email,
                "address" to address,
                "bikeCategory" to bikeCategory,
                "isDemo" to false,
                "registrationDate" to FieldValue.serverTimestamp()
            )
            ref.set(data).addOnCompleteListener { save ->
                if (save.isSuccessful) onResult(true, customerId, null)
                else onResult(false, null, firestoreErrorMessage(save.exception))
            }
        }
    }

    /** Loads the logged-in user's own registration. customer is null if not registered yet. */
    fun getMyCustomer(onResult: (customer: Customer?, error: String?) -> Unit) {
        val user = currentUser()
        if (user == null) {
            onResult(null, "You are not logged in.")
            return
        }
        db.collection(CUSTOMERS).document(user.uid).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val doc = task.result
                if (doc != null && doc.exists()) onResult(docToCustomer(doc), null)
                else onResult(null, null)
            } else {
                onResult(null, firestoreErrorMessage(task.exception))
            }
        }
    }

    /** An admin is a user whose UID has a document in the "admins" collection. */
    fun checkAdmin(uid: String, onResult: (isAdmin: Boolean) -> Unit) {
        db.collection(ADMINS).document(uid).get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    /** Admin gets every record; a normal user gets only their own. */
    fun loadRecords(
        isAdmin: Boolean,
        uid: String,
        onResult: (records: List<Customer>?, error: String?) -> Unit
    ) {
        val query: Query = if (isAdmin) db.collection(CUSTOMERS)
        else db.collection(CUSTOMERS).whereEqualTo("uid", uid)

        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val list = task.result?.documents?.map { docToCustomer(it) } ?: emptyList()
                onResult(list.sortedBy { it.fullName.lowercase() }, null)
            } else {
                onResult(null, firestoreErrorMessage(task.exception))
            }
        }
    }

    // Manual mapping (safer than toObject() for a Boolean field named isDemo)
    private fun docToCustomer(d: DocumentSnapshot): Customer = Customer(
        uid = d.getString("uid") ?: "",
        customerId = d.getString("customerId") ?: "",
        fullName = d.getString("fullName") ?: "",
        phoneNumber = d.getString("phoneNumber") ?: "",
        dateOfBirth = d.getString("dateOfBirth") ?: "",
        email = d.getString("email") ?: "",
        address = d.getString("address") ?: "",
        bikeCategory = d.getString("bikeCategory") ?: "",
        isDemo = d.getBoolean("isDemo") ?: false
    )

    // ------------------------------------------------------- DEMO RECORDS

    // Fictional data. Each row: name, phone, email, DOB, address, bike category
    private val demoRows = listOf(
        listOf("Rahul Sharma", "9876543210", "rahul.demo@example.com", "15/03/1999",
            "12, Shanti Nagar Society, Navrangpura, Ahmedabad, Gujarat 380009", "Commuter Bike"),
        listOf("Priya Patel", "9876501234", "priya.demo@example.com", "22/07/2000",
            "45, Sunrise Apartments, Satellite, Ahmedabad, Gujarat 380015", "Scooter"),
        listOf("Amit Shah", "9876512345", "amit.demo@example.com", "09/11/1998",
            "7, Green Park Colony, Maninagar, Ahmedabad, Gujarat 380008", "Sports Bike"),
        listOf("Neha Verma", "9876523456", "neha.demo@example.com", "30/01/2001",
            "203, Lake View Residency, Vastrapur, Ahmedabad, Gujarat 380054", "Scooter"),
        listOf("Karan Mehta", "9876534567", "karan.demo@example.com", "18/05/1997",
            "88, Royal Heights, Bopal, Ahmedabad, Gujarat 380058", "Commuter Bike")
    )

    /**
     * Writes 5 demo records using FIXED document IDs (demo_1 ... demo_5).
     * Running it again just overwrites the same 5 documents, so duplicates are impossible.
     * Only an admin is allowed to do this (enforced by the Firestore rules).
     */
    fun seedDemoRecords(onResult: (success: Boolean, error: String?) -> Unit) {
        val batch = db.batch()
        demoRows.forEachIndexed { index, row ->
            val docId = "demo_${index + 1}"
            val data = hashMapOf<String, Any>(
                "uid" to "demo-user",
                "customerId" to "DEMO-00${index + 1}",
                "fullName" to row[0],
                "phoneNumber" to row[1],
                "email" to row[2],
                "dateOfBirth" to row[3],
                "address" to row[4],
                "bikeCategory" to row[5],
                "isDemo" to true,
                "registrationDate" to FieldValue.serverTimestamp()
            )
            batch.set(db.collection(CUSTOMERS).document(docId), data)
        }
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(true, null)
            else onResult(false, firestoreErrorMessage(task.exception))
        }
    }

    // ------------------------------------------------------ ERROR MESSAGES

    private fun authErrorMessage(e: Exception?): String = when (e) {
        is FirebaseNetworkException -> "No internet connection. Please check your network and try again."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException -> "No account found with this email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        else -> e?.localizedMessage ?: "Something went wrong. Please try again."
    }

    private fun firestoreErrorMessage(e: Exception?): String {
        if (e is FirebaseFirestoreException) {
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied. Check the Firestore security rules."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Please check your network and try again."
                else -> e.localizedMessage ?: "Database error. Please try again."
            }
        }
        return e?.localizedMessage ?: "Something went wrong. Please try again."
    }
}
