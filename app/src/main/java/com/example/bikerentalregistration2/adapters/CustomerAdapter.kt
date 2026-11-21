package com.example.bikerentalregistration2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerentalregistration2.databinding.ItemCustomerBinding
import com.example.bikerentalregistration2.models.Customer

/** Shows a list of Customer objects in a RecyclerView. */
class CustomerAdapter : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    private var items: List<Customer> = emptyList()

    class ViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        val b = holder.binding
        b.tvName.text = c.fullName
        b.tvDemo.visibility = if (c.isDemo) View.VISIBLE else View.GONE
        b.tvPhone.text = "Phone: ${c.phoneNumber}"
        b.tvEmail.text = "Email: ${c.email}"
        b.tvDob.text = "Date of Birth: ${c.dateOfBirth}"
        b.tvAddress.text = "Address: ${c.address}"
        b.tvBike.text = "Bike Category: ${c.bikeCategory}"
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Customer>) {
        items = newItems
        notifyDataSetChanged()
    }
}
