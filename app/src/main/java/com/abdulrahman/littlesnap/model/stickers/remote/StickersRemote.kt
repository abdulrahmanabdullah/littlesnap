package com.abdulrahman.littlesnap.model.stickers.remote

import android.util.Log
import com.abdulrahman.littlesnap.model.stickers.Stickers
import com.abdulrahman.littlesnap.model.stickers.StickersDataSource
import com.google.firebase.database.*
import kotlinx.coroutines.*

private const val TAG = "Remote"

class StickersRemote : StickersDataSource {


    suspend fun getDeferredList(): List<Stickers> {
        val listOfStickers = mutableListOf<Stickers>()
        return GlobalScope.async(Dispatchers.IO) {
            withContext(this.coroutineContext) {
                FirebaseDatabase.getInstance().getReference("stickers")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            Log.i("xyz", "Cancelled database ${p0.message}.")
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            p0.children.forEach {
                                val sticker = it.getValue(Stickers::class.java)
                                listOfStickers.add(sticker!!)
                            }
                        }

                    })
                return@withContext listOfStickers
            }
        }.await()

    }


    fun Query.toDeferred(): Deferred<DataSnapshot> {
        val deferred = CompletableDeferred<DataSnapshot>()
        this.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.i("xyz","onCancelled throw exception ${p0.message}")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                 deferred.complete(dataSnapshot)

                //Also try this , but not work
//                dataSnapshot.children.forEach {
//                    val obj = it.getValue(Stickers::class.java)
//                    listOfStickers.add(obj!!)
//                    deferred.complete(dataSnapshot)
//                }
            }
        })
        return deferred
    }

    val listOfStickers = mutableListOf<Stickers>()

    fun fetchStickers() : Deferred<List<Stickers>> {
        val ref = FirebaseDatabase.getInstance().getReference("stickers")
        return GlobalScope.async(Dispatchers.IO) {
             withContext(this.coroutineContext) {
                val data = ref.toDeferred().await()
                val sticker = data.getValue(Stickers::class.java)
                 listOfStickers.add(sticker!!)
                return@withContext listOfStickers
            }
        }
    }
}