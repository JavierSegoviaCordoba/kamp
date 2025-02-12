package app.service

import app.domain.LibraryCount
import app.domain.PagedResponse
import app.util.buildNextUrl
import app.util.buildPrevUrl
import io.ktor.application.ApplicationCall
import kamp.domain.KotlinMPPLibrary
import org.litote.kmongo.MongoOperator.all
import org.litote.kmongo.MongoOperator.and
import org.litote.kmongo.MongoOperator.or
import org.litote.kmongo.MongoOperator.regex
import org.litote.kmongo.ascending
import org.litote.kmongo.coroutine.CoroutineCollection

actual class LibraryService(
  private val call: ApplicationCall,
  private val collection: CoroutineCollection<KotlinMPPLibrary>,
) {
  private fun buildQuery(search: String?, targets: Set<String>?): String {
    val searchQuery = search?.let {
      """
      {
        $or: [
         {name: {$regex: /$search/i}},
         {group: {$regex: /$search/i}},
         {description: {$regex: /$search/i}},
        ]
      }
      """.trimIndent()
    }
    val targetsQuery = targets?.let {
      """
      {
        'targets.platform': {
          $all: [${targets.joinToString(",") { "'$it'" }}]
        }
      }
      """.trimIndent()
    }
    val query = setOfNotNull(searchQuery, targetsQuery).takeIf { it.isNotEmpty() }?.let {
      """
      {
        $and: [${it.joinToString(",")}]
      }
      """.trimIndent()
    } ?: "{}"
    return query
  }
  
  actual suspend fun getAll(page: Int, size: Int, search: String?, targets: Set<String>?): PagedResponse<KotlinMPPLibrary> {
    val data = collection.find(buildQuery(search, targets)).sort(ascending(KotlinMPPLibrary::name)).skip(size * (page - 1)).limit(size).toList()
    return PagedResponse(
      data = data,
      page = page,
      next = call.request.buildNextUrl(data.size),
      prev = call.request.buildPrevUrl()
    )
  }
  
  actual suspend fun getCount(search: String?, targets: Set<String>?): LibraryCount {
    return LibraryCount(collection.countDocuments(buildQuery(search, targets)))
  }
  
  suspend fun create(library: KotlinMPPLibrary) {
    collection.save(library)
  }
  
  actual companion object
}
