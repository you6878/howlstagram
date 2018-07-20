package com.company.howl.howlstagram.navigation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.company.howl.howlstagram.R
import com.company.howl.howlstagram.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.ArrayList


class AlarmFragment : Fragment() {

    var alarmSnapshot: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)
        view.alarmframgent_recyclerview.adapter = AlarmRecyclerViewAdapter()
        view.alarmframgent_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val alarmDTOList = ArrayList<AlarmDTO>()

        init {

            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            println(uid)
            FirebaseFirestore.getInstance()
                    .collection("alarms")
                    .whereEqualTo("destinationUid", uid)
                    .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        alarmDTOList.clear()
                        if(querySnapshot == null)return@addSnapshotListener
                        for (snapshot in querySnapshot?.documents!!) {
                            alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                        }
                        alarmDTOList.sortByDescending { it.timestamp }
                        notifyDataSetChanged()
                    }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val profileImage = holder.itemView.commentviewitem_imageview_profile
            val commentTextView = holder.itemView.commentviewitem_textview_profile

            FirebaseFirestore.getInstance().collection("profileImages")
                    .document(alarmDTOList[position].uid!!).get().addOnCompleteListener {
                        task ->
                        if(task.isSuccessful){
                            val url = task.result["image"]
                            Glide.with(activity)
                                    .load(url)
                                    .apply(RequestOptions().circleCrop())
                                    .into(profileImage)
                        }
                    }

            when (alarmDTOList[position].kind) {
                0 -> {
                    val str_0 = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    commentTextView.text = str_0
                }

                1 -> {
                    val str_1 = alarmDTOList[position].userId + getString(R.string.alarm_who) + alarmDTOList[position].message + getString(R.string.alarm_comment)
                    commentTextView.text = str_1
                }

                2 -> {
                    val str_2 = alarmDTOList[position].userId + getString(R.string.alarm_follow)
                    commentTextView.text = str_2
                }
            }
        }

        override fun getItemCount(): Int {

            return alarmDTOList.size
        }
        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    }
}