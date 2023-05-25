package com.example.kotlinebicatarider

import android.app.Activity
import android.widget.TextView
import android.os.Handler
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.kotlinebicatarider.Model.RiderModel
import com.example.kotlinebicatarider.commo.commo
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.common.internal.service.Common
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.nio.channels.spi.AsynchronousChannelProvider
import java.sql.Driver
import java.util.Arrays
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {
    companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }
    lateinit var providers: List<AuthUI.IdpConfig>
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var listener: AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    private fun delaySplashScreen(){
        Completable.timer(3, TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })

    }

    override fun onStop(){
        if(firebaseAuth !=null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()
    }

    private fun init(){
        database = FirebaseDatabase.getInstance()

        riderInfoRef = database.getReference(commo.RIDER_GET_REFERENCE)
        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaeAuth ->
            val user = myFirebaeAuth.currentUser
            if(user != null)
            {
                checkUserFromFirebase()
            }
            else
            {
                showLoginLayout()
            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(provider)
            .setIsSmartLockEnabled(false)
            .build(), LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== LOGIN_REQUEST_CODE)
        {
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK)
            {
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
            {
                Toast.makeText(this@SplashScreenActivity, response!!.error!!.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,p0.message, Toast.LENGTH_LONG).show()
                }
                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists())
                    {
                        val model = p0.getValue(RiderModel::class.java)
                        gotoHomeActivity(model)
                    }
                    else
                    {
                        showRegisterLayout()
                    }
                }



            })

    }

    private fun showRegisterLayout() {
        val builder =AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val edt_first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btn_continue.setOnClickListener{
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter First Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Last Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Phone Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else
            {

                val model= RiderModel()
                model.firstName = edt_first_name.text.toString()
                model.lastName = edt_last_name.text.toString()
                model.phoneNumber = edt_phone_number.text.toString()


                riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{ e ->
                        Toast.makeText(this@SplashScreenActivity,""+e.message,Toast.LENGTH_SHORT).show()
                        dialog.dismiss()




                    }
                    .addOnSuccessListener{
                        Toast.makeText(this@SplashScreenActivity,"Register Sucessfully!",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        gotoHomeActivity(model)
                        p.visibility=View.GONE




                    }
            }
        }
    }

    private fun gotoHomeActivity(model: RiderModel?) {
        commo.currentRider = model

    }
}