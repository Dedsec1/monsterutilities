package xerus.monstercat.downloader

import com.google.common.io.CountingInputStream
import javafx.concurrent.Task
import xerus.ktutil.createDirs
import xerus.ktutil.replaceIllegalFileChars
import xerus.monstercat.api.APIConnection
import xerus.monstercat.api.response.MusicResponse
import xerus.monstercat.api.response.Release
import xerus.monstercat.api.response.Track
import xerus.monstercat.logger
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipInputStream

fun MusicResponse.downloader() = when(this) {
	is Track -> TrackDownloader(this)
	is Release -> ReleaseDownloader(this)
	else -> throw NoWhenBranchMatchedException()
}

fun Release.path() = basepath.resolve(when {
	isMulti -> toString(ALBUMFOLDER()).replaceIllegalFileChars() // Album, Monstercat Collection
	isType("Podcast", "Mixes") -> type
	else -> SINGLEFOLDER() // Single
}).createDirs()

fun Track.path() = basepath.resolve(SINGLEFOLDER()).createDirs().resolve(addFormat(toFileName()))

private inline val basepath
	get() = DOWNLOADDIR()

private fun addFormat(fileName: String) = "$fileName.${QUALITY().split('_')[0]}"

abstract class Downloader(title: String, val coverUrl: String) : Task<Unit>() {
	
	init {
		@Suppress("LEAKINGTHIS")
		updateTitle(title)
	}
	
	override fun call() {
		try {
			download()
		} catch (e: Exception) {
			logger.throwing("Downloader", "download", e)
			updateMessage("Error: $e")
			try {
				Thread.sleep(100_000_000_000)
			} catch (_: Exception) {
			}
		} finally {
			abort()
		}
	}
	
	abstract fun download()
	
	private var length: Long = 0
	
	private lateinit var con: APIConnection
	private lateinit var cis: CountingInputStream
	
	protected fun <T : InputStream> createConnection(releaseId: String, streamConverter: (InputStream) -> T, vararg queries: String): T {
		con = APIConnection("release", releaseId, "download").addQueries("method=download", "type=" + QUALITY(), *queries)
		val entity = con.getResponse().entity
		length = entity.contentLength
		if (length == 0L)
			throw EmptyResponseException()
		updateProgress(0, length)
		val input = streamConverter(entity.content)
		cis = CountingInputStream(input)
		return input
	}
	
	protected fun abort() = con.abort()
	
	private val buffer = ByteArray(1024)
	protected fun downloadFile(path: Path) {
		val file = path.toFile()
		logger.finest("Downloading $file")
		updateMessage(file.name)
		val output = FileOutputStream(file)
		try {
			while (true) {
				val length = cis.read(buffer)
				if (length < 1) return
				output.write(buffer, 0, length)
				updateProgress(cis.count)
				if (isCancelled) {
					output.close()
					file.delete()
					return
				}
			}
		} catch (e: Exception) {
			logger.throwing("Downloader", "downloadFile", e)
		} finally {
			output.close()
		}
	}
	
	private fun updateProgress(progress: Long) = updateProgress(progress, length)
	
}

class ReleaseDownloader(private val release: Release) : Downloader(release.toString(), release.coverUrl) {
	
	override fun download() {
		val path = release.path().createDirs()
		logger.finer("Downloading $release to $path")
		
		val zis = createConnection(release.id, { ZipInputStream(it) })
		zip@ do {
			val entry = zis.nextEntry ?: break
			if (entry.isDirectory)
				continue
			val name = entry.name.split("/").last()
			if (name == "cover.false" || !release.isMulti && name.contains("cover."))
				continue
			val target = when {
				name.contains("cover.") -> path.resolve(name)
				name.contains("Album Mix") ->
					when (ALBUMMIXES()) {
						"Separate" -> resolve(name, basepath.resolve("Album Mixes").createDirs())
						"Exclude" -> continue@zip
						else -> resolve(name, path)
					}
				else -> resolve(name, path)
			}
			downloadFile(target)
		} while (!isCancelled)
	}
	
	private fun resolve(name: String, path: Path = basepath): Path =
			path.resolve(addFormat(ReleaseFile(name, !release.isMulti).toFileName()))
	
}

class TrackDownloader(private val track: Track) : Downloader(track.toString(), APIConnection("catalog", "release", track.alb.albumId).parseJSON(Release::class.java).coverUrl) {
	
	override fun download() {
		createConnection(track.alb.albumId, { it }, "track=" + track.id)
		downloadFile(track.path())
		abort()
	}
	
}

class EmptyResponseException : Exception("No file found!")
