package xerus.monstercat.test

import javafx.scene.layout.VBox
import xerus.ktutil.printNamed
import java.net.URL


object Test : VBox() {
	
	@JvmStatic
	fun main(args: Array<String>) {
		val version = URL("http://monsterutilities.bplaced.net/version").openConnection().getInputStream().reader().readLines().first().printNamed()
		version.length.printNamed()
	}
	
}
