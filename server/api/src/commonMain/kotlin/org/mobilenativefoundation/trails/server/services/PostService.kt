package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.CommentRecord
import org.mobilenativefoundation.trails.server.model.CreatePostRequest
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.model.UpdatePostRequest
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest

interface PostService {
    suspend fun createPost(token: String, create: CreatePostRequest): SkiRunPostRecord
    suspend fun getPost(postId: String): SkiRunPostRecord
    suspend fun getPost(postId: String, token: String): SkiRunPostRecord
    suspend fun updatePost(token: String, postId: String, update: UpdatePostRequest): SkiRunPostRecord
    suspend fun deletePost(token: String, postId: String)

    suspend fun likePost(token: String, postId: String)
    suspend fun unlikePost(token: String, postId: String)
    suspend fun bookmarkPost(token: String, postId: String)
    suspend fun unbookmarkPost(token: String, postId: String)

    suspend fun getComments(postId: String, request: CursorRequest): CursorPage<CommentRecord>
    suspend fun addComment(token: String, postId: String, body: String): CommentRecord
    suspend fun deleteComment(token: String, postId: String, commentId: String)

    suspend fun getBookmarkedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord>
    suspend fun getLikedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord>
}
