package com.example.catcom.domain.repository

import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Comment
import com.example.catcom.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getFeed(): Flow<Result<List<Post>>>
    fun createPost(content: String, imageUri: Uri?): Flow<Result<Boolean>>
    fun toggleLike(postId: String): Flow<Result<Boolean>>
    fun getComments(postId: String): Flow<Result<List<Comment>>>
    fun sendComment(postId: String, text: String): Flow<Result<Boolean>>
    fun getUserPosts(userId: String): Flow<Result<List<Post>>>
    fun searchPosts(query: String): Flow<Result<List<Post>>>
}
