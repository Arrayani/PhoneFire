package academy.learnprogramming

import academy.learnprogramming.databinding.ActivityMainBinding
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    //if code sending failed,wiill used to resend
    private var forceResendingToken:PhoneAuthProvider.ForceResendingToken?=null
    private var mCallbacks:PhoneAuthProvider.OnVerificationStateChangedCallbacks?=null
    private var mVerificationId:String?=null
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG ="MAIN_TAG"

    //Progress Dialog
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phoneLl.visibility =  View.VISIBLE
        binding.codeLl.visibility = View.GONE

        firebaseAuth =  FirebaseAuth.getInstance()
        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity,"${e.message}",Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d(TAG,"onCodeSent:$verificationId")
                mVerificationId = verificationId
                forceResendingToken =  token
                progressDialog.dismiss()

                //hide phone layout ,show code layout
                binding.phoneLl.visibility=View.VISIBLE
                binding.codeEt.visibility=View.GONE
                Toast.makeText(this@MainActivity,"Verification code sent....",Toast.LENGTH_SHORT).show()
                binding.codeSentDescriptionTv.text = "Please type the verification code we sent to ${binding.phoneEt.text.toString().trim()}"

            }
        }
            //masih dalam callback buat ngehanndle clicknnya
        binding.phoneContinueBtn.setOnClickListener{
        val phone =  binding.phoneEt.text.toString().trim()
            if (TextUtils.isEmpty(phone)){
                Toast.makeText(this@MainActivity,"Please enter phone number",Toast.LENGTH_SHORT).show()
            }else{
                startPhoneNumberVerification(phone)
            }
        }
        binding.resendCodeTv.setOnClickListener {
            val phone =  binding.phoneEt.text.toString().trim()
            if (TextUtils.isEmpty(phone)){
                Toast.makeText(this@MainActivity,"Please enter phone number",Toast.LENGTH_SHORT).show()
            }else{
                resendVerificationCode(phone,forceResendingToken)
            }
        }
        binding.codeSubmitBtn.setOnClickListener{
            val code=binding.codeEt.text.toString().trim()
            if (TextUtils.isEmpty(code)){
                Toast.makeText(this@MainActivity,"Please enter verification code",Toast.LENGTH_SHORT).show()
            }else{verifyPhoneNumberWithCode(mVerificationId,code)

            }
        }

    }

    private fun startPhoneNumberVerification(phone:String){
        progressDialog.setMessage("Verify Phone Number...")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(phone: String,token:PhoneAuthProvider.ForceResendingToken?){
        progressDialog.setMessage("Resending Code...")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks!!)
            .setForceResendingToken(token!!) // bedanya ini sama yg diatas
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun verifyPhoneNumberWithCode(verificationId:String?,code:String){
        progressDialog.setMessage("Verifying Code....")
        progressDialog.show()

        val credential =  PhoneAuthProvider.getCredential(verificationId.toString(),code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressDialog.setMessage("Logging in")
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
            progressDialog.dismiss()
                val phone = firebaseAuth.currentUser?.phoneNumber
                Toast.makeText(this,"Logged in as $phone",Toast.LENGTH_SHORT).show()

                //open activity
                startActivity(Intent(this,ProfileActivity::class.java))
                finish()
            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this,"${e.message}",Toast.LENGTH_SHORT).show()
            }
    }
}
