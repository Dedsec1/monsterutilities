package xerus.monstercat.test

import javafx.scene.layout.VBox
import xerus.ktutil.containsAny
import xerus.ktutil.printNamed
import xerus.monstercat.api.APIConnection
import java.io.File
import java.net.URL


object Test : VBox() {
	
	@JvmStatic
	fun main(args: Array<String>) {
		val response = APIConnection("catalog", "release").getReleases()
		val file = File("allreleases.txt")
		if(response == null) {
			println("No releases found!")
			return
		}
		file.outputStream().bufferedWriter().use {
			response.sortedBy { it.renderedArtists }.forEach { r ->
				if(r.isType("Single", "Album"))
					it.appendln(r.toString())
			}
		}
		println("Done to $file")
	}
	
}
