package com.example.stockmanagement.customers

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_PHONE
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stockmanagement.GetListOfData
import com.example.stockmanagement.ManagementDao
import com.example.stockmanagement.ManagementDatabase
import com.example.stockmanagement.R
import kotlinx.coroutines.launch

class CustomerList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomerListAdapter
    private lateinit var dao: ManagementDao
    private lateinit var recordCount:TextView

    enum class FilterType {
        NONE, BALANCE, NAME_ASC, NAME_DESC, CUSTOMER_NAME, PHONE_NUMBER
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back_icon)

        // Initialize RecyclerView and adapter with an empty list
        recyclerView = findViewById(R.id.RV_CustomerList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CustomerListAdapter(mutableListOf(), this@CustomerList) { customer ->
            val nextScreen = Intent(this@CustomerList, CustomerView::class.java)
            nextScreen.putExtra("CUSTOMER_ID", customer.cid)
            startActivity(nextScreen)
        }
        recyclerView.adapter = adapter

        // Initialize DAO
        dao = ManagementDatabase.getInstance(this).managementDao

        // Load initial data
        loadCustomerList()

        val filterDropdown = findViewById<Spinner>(R.id.Spi_PurchaseFilter)
        val clearButton = findViewById<Button>(R.id.Btn_FilterClear)
        val defaultColor = clearButton.backgroundTintList
        val selectedTextView = findViewById<TextView>(R.id.TV_SelectedText)
        recordCount = findViewById(R.id.list_record_count)
        selectedTextView.visibility = View.GONE

        val filterMap = mapOf(
            getString(R.string.filter_customer_none) to FilterType.NONE,
            getString(R.string.filter_customer_balance) to FilterType.BALANCE,
            getString(R.string.filter_customer_name_asc) to FilterType.NAME_ASC,
            getString(R.string.filter_customer_name_desc) to FilterType.NAME_DESC,
            getString(R.string.filter_customer_name) to FilterType.CUSTOMER_NAME,
            getString(R.string.filter_customer_phone_number) to FilterType.PHONE_NUMBER
        )

        val filterList = filterMap.keys.toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterDropdown.adapter = spinnerAdapter

        filterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, p1: View?, position: Int, id: Long) {
                val selectedLabel = parent.getItemAtPosition(position).toString()
                val selectedFilter = filterMap[selectedLabel] ?: FilterType.NONE

                if (selectedFilter != FilterType.NONE) {
                    clearButton.visibility = View.VISIBLE
                    clearButton.backgroundTintList = ColorStateList.valueOf(Color.RED)

                    when (selectedFilter) {
                        FilterType.BALANCE -> {
                            lifecycleScope.launch {
                                val customers = dao.getAllCustomersSortByBalance()
                                recordCount.text=customers.size.toString()
                                adapter.updateData(customers)
                            }
                        }
                        FilterType.NAME_ASC -> {
                            lifecycleScope.launch {
                                val customers = dao.getAllCustomerSortByNameAsc()
                                recordCount.text=customers.size.toString()
                                adapter.updateData(customers)
                            }
                        }
                        FilterType.NAME_DESC -> {
                            lifecycleScope.launch {
                                val customers = dao.getAllCustomerSortByNameDesc()
                                recordCount.text=customers.size.toString()
                                adapter.updateData(customers)
                            }
                        }
                        FilterType.CUSTOMER_NAME -> {
                            showCustomerNameFilterDialog()
                        }
                        FilterType.PHONE_NUMBER -> {
                            showCustomerPhoneNoDialog()
                        }
                        else -> {}
                    }
                } else {
                    clearButton.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        clearButton.setOnClickListener {
            clearButton.backgroundTintList = defaultColor
            filterDropdown.setSelection(0)
            selectedTextView.visibility = View.GONE
            clearButton.visibility= View.GONE
            loadCustomerList()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun showCustomerPhoneNoDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.filter_popup_Enter_Here)
            inputType = TYPE_CLASS_PHONE
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_input_padding)
            setPadding(padding, padding, padding, padding)
            filters = arrayOf(InputFilter.LengthFilter(10))
            keyListener = DigitsKeyListener.getInstance("0123456789")
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.filter_customer_popup_enter_phone))
            .setView(input)
            .setPositiveButton(getString(R.string.popup_apply)) { dialog, _ ->
                val phoneNo = input.text.toString().toIntOrNull()
                val selectedText = findViewById<TextView>(R.id.TV_SelectedText)
                if (phoneNo != null && phoneNo.toString().length >=3 ) {
                    selectedText.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        selectedText.text= "${getString(R.string.filter_customer_popup_entered_phone)}: $phoneNo"
                       val customer = dao.getAllCustomersPhone(phoneNo.toString())
                        recordCount.text=customer.size.toString()
                        if(customer.isNotEmpty()){
                            adapter.updateData(customer)
                        } else{
                            adapter.updateData(customer)
                            selectedText.text = getString(R.string.filter_result_customer)
                        }
                    }
                }
                else{
                    Toast.makeText(this@CustomerList,getString(R.string.filter_customer_popup_phone_validation), Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.popup_cancel)) { dialog, _ -> dialog.cancel() }
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showCustomerNameFilterDialog() {
        val input = AutoCompleteTextView(this).apply {
            hint = getString(R.string.filter_popup_Enter_Here)
            threshold = 1
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_input_padding)
            setPadding(padding, padding, padding, padding)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.filter_customer_popup_enter_name))
            .setView(input)
            .setPositiveButton(getString(R.string.popup_apply)) { dialog, _ ->
                val name = input.text.toString().trim()
                val selectedText = findViewById<TextView>(R.id.TV_SelectedText)
                if (name.isNotEmpty()) {
                    selectedText.text = "${getString(R.string.filter_customer_popup_entered_customer)}: $name"
                    selectedText.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val customer= dao.getCustomerByname(name)
                        if(customer != null){
                            recordCount.text="1"
                            adapter.updateData(listOf(customer))
                        } else{
                            recordCount.text="0"
                            adapter.updateData(emptyList())
                            selectedText.text = getString(R.string.filter_result_customer)
                        }
                    }
                } else {
                    selectedText.visibility = View.GONE
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.popup_cancel)) { dialog, _ -> dialog.cancel() }
            .show()
        GetListOfData(this, this).getAllCustomerNames{ names ->
            input.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }


    override fun onResume() {
        super.onResume()
        loadCustomerList()
    }

    private fun loadCustomerList() {
        lifecycleScope.launch {
            val customers = dao.getAllCustomer()
            recordCount.text=customers.size.toString()
            adapter.updateData(customers.toMutableList())
        }
    }
}
