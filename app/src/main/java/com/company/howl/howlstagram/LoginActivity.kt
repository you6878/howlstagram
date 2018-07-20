package com.company.howl.howlstagram


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*


class LoginActivity : AppCompatActivity() {

    // Firebase Authentication 관리 클래스
    var auth: FirebaseAuth? = null

    // GoogleLogin 관리 클래스
    var googleSignInClient: GoogleSignInClient? = null

    // Facebook 로그인 처리 결과 관리 클래스
    var callbackManager: CallbackManager? = null

    //GoogleLogin
    val GOOGLE_LOGIN_CODE = 9001 // Intent Request ID

    //TwitterLogin
    var twitterAuthClient: TwitterAuthClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Twitter.initialize(this)
        setContentView(R.layout.activity_login)

        // Firebase 로그인 통합 관리하는 Object 만들기
        auth = FirebaseAuth.getInstance()

        //구글 로그인 옵션
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        //구글 로그인 클래스를 만듬
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

        //트위터 세팅
        twitterAuthClient = TwitterAuthClient()

        //구글 로그인 버튼 세팅
        google_sign_in_button.setOnClickListener { googleLogin() }

        //페이스북 로그인 세팅
        facebook_login_button.setOnClickListener { facebookLogin() }

        //이메일 로그인 세팅
        email_login_button.setOnClickListener { emailLogin() }

        //트위터 로그인 세팅
        twitter_login_button.setOnClickListener { twitterLogin() }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook SDK로 값 넘겨주기
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        //Twitter SDK로 값 넘겨주기
        twitterAuthClient?.onActivityResult(requestCode, resultCode, data)

        // 구글에서 승인된 정보를 가지고 오기
        if (requestCode == GOOGLE_LOGIN_CODE && resultCode == Activity.RESULT_OK) {

            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            println(result.status.toString())
            if (result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                progress_bar.visibility = View.GONE
            }
        }
    }

    fun googleLogin() {
        progress_bar.visibility = View.VISIBLE
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun moveMainPage(user: FirebaseUser?) {

        // User is signed in
        if (user != null) {
            Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


    fun facebookLogin() {
        progress_bar.visibility = View.VISIBLE
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                progress_bar.visibility = View.GONE
            }

            override fun onError(error: FacebookException) {
                progress_bar.visibility = View.GONE
            }
        })
    }

    fun twitterLogin() {
        progress_bar.visibility = View.VISIBLE
        twitterAuthClient?.authorize(this, object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>?) {
                val credential = TwitterAuthProvider.getCredential(
                        result?.data?.authToken?.token!!,
                        result?.data?.authToken?.secret!!)
                auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE
                    //다음 페이지 이동
                    if (task.isSuccessful) {
                        moveMainPage(auth?.currentUser)
                    }
                }
            }

            override fun failure(exception: TwitterException?) {
                println(exception.toString())
            }
        })
    }

    // Facebook 토큰을 Firebase로 넘겨주는 코드
    fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth?.signInWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE
                    //다음 페이지 이동
                    if (task.isSuccessful) {
                        moveMainPage(auth?.currentUser)
                    }
                }
    }

    //이메일 회원가입 및 로그인 메소드
    fun createAndLoginEmail() {

        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                ?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE
                    if (task.isSuccessful) {
                        //아이디 생성이 성공했을 경우
                        Toast.makeText(this,
                                getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()

                        //다음페이지 호출
                        moveMainPage(auth?.currentUser)
                    } else if (task.exception?.message.isNullOrEmpty()) {
                        //회원가입 에러가 발생했을 경우
                        Toast.makeText(this,
                                task.exception!!.message, Toast.LENGTH_SHORT).show()
                    } else {
                        //아이디 생성도 안되고 에러도 발생되지 않았을 경우 로그인
                        signinEmail()
                    }
                }

    }

    fun emailLogin() {

        if (email_edittext.text.toString().isNullOrEmpty() || password_edittext.text.toString().isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()

        } else {

            progress_bar.visibility = View.VISIBLE
            createAndLoginEmail()

        }
    }

    //로그인 메소드
    fun signinEmail() {

        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                ?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE

                    if (task.isSuccessful) {
                        //로그인 성공 및 다음페이지 호출
                        moveMainPage(auth?.currentUser)
                    } else {
                        //로그인 실패
                        Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                    }
                }

    }


    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE
                    if (task.isSuccessful) {


                        //다음페이지 호출
                        moveMainPage(auth?.currentUser)
                    }
                }
    }


    override fun onStart() {
        super.onStart()

        //자동 로그인 설정
        moveMainPage(auth?.currentUser)

    }
}
