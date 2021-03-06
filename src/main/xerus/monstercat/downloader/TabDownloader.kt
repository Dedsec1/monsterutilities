package xerus.monstercat.downloader

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.experimental.*
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.controlsfx.control.CheckTreeView
import org.controlsfx.control.SegmentedButton
import org.controlsfx.control.TaskProgressView
import org.controlsfx.dialog.ProgressDialog
import xerus.ktutil.helpers.Cache
import xerus.ktutil.helpers.PseudoParser.ParserException
import xerus.ktutil.javafx.*
import xerus.ktutil.javafx.properties.*
import xerus.ktutil.javafx.ui.App
import xerus.ktutil.javafx.ui.FileChooser
import xerus.ktutil.javafx.ui.FilterableTreeItem
import xerus.ktutil.javafx.ui.controls.*
import xerus.ktutil.toLocalDate
import xerus.monstercat.api.*
import xerus.monstercat.api.response.*
import xerus.monstercat.logger
import xerus.monstercat.monsterUtilities
import xerus.monstercat.tabs.VTab
import java.time.LocalDate
import java.util.concurrent.Executors

private val qualities = arrayOf("mp3_128", "mp3_v2", "mp3_v0", "mp3_320", "flac", "wav")
val trackPatterns = ImmutableObservableList("%artistsTitle% - %title%", "%artists|, % - %title%", "%artists|enumeration% - %title%", "%artists|, % - %titleRaw%{ (feat. %feat%)}{ [%remix%]}")
val albumTrackPatterns = ImmutableObservableList("%artistsTitle% - %album% - %track% %title%", "%artists|enumeration% - %title% - %album%", *trackPatterns.content)

val TreeItem<out MusicResponse>.normalisedValue
	get() = value.toString().trim().toLowerCase()

class TabDownloader : VTab() {
	
	private val releaseView = ReleaseView()
	private val trackView = TrackView()
	
	private val patternValid = SimpleObservable(false)
	private val noItemsSelected = SimpleObservable(true)
	
	private val searchField = TextField()
	private val releaseSearch = SearchRow(Searchable<Release, LocalDate>("Releases", Type.DATE, { it.releaseDate.substring(0, 10).toLocalDate() }))
	
	init {
		FilterableTreeItem.autoExpand = true
		FilterableTreeItem.autoLeaf = false
		
		// Check if views are empty
		val itemListener = InvalidationListener { noItemsSelected.value = releaseView.checkedItems.size + trackView.checkedItems.size == 0 }
		releaseView.checkedItems.addListener(itemListener)
		trackView.checkedItems.addListener(itemListener)
		
		initialize()
	}
	
	private fun initialize() {
		children.clear()
		
		// Download directory
		val chooser = FileChooser(App.stage, DOWNLOADDIR().toFile(), null, "Download directory")
		chooser.selectedFile.listen { DOWNLOADDIR.set(it.toPath()) }
		add(HBox(chooser.textField(), createButton("Select Folders") {
			Stage().run {
				val pane = gridPane(padding = 5)
				scene = Scene(pane)
				pane.addRow(0, chooser.textField(), chooser.button().allowExpand(vertical = false))
				pane.addRow(1, Label("Singles Subfolder").centerText(), TextField().bindText(SINGLEFOLDER).placeholder("Subfolder (root if empty)"))
				pane.addRow(2, Label("Albums Subfolder").centerText(), TextField().bindText(ALBUMFOLDER).placeholder("Subfolder (root if empty)"))
				pane.children.forEach {
					GridPane.setVgrow(it, Priority.SOMETIMES)
					GridPane.setHgrow(it, if (it is TextField) Priority.ALWAYS else Priority.SOMETIMES)
				}
				initWindowOwner(App.stage)
				pane.prefWidth = 700.0
				initStyle(StageStyle.UTILITY)
				isResizable = false
				show()
			}
		}))
		
		// Patterns
		val patternPane = gridPane()
		fun patternLabel(pattern: Property<String>, track: Track) =
				Label().apply {
					textProperty().dependOn(pattern) {
						try {
							track.toString(pattern.value).also { patternValid.value = true }
						} catch (e: ParserException) {
							patternValid.value = false
							"No such field: " + e.cause?.cause?.message
						} catch (e: Exception) {
							patternValid.value = false
							monsterUtilities.showError(e, "Error parsing pattern")
							e.toString()
						}
					}
				}
		patternPane.add(Label("Singles pattern"),
				0, 0, 1, 2)
		patternPane.add(ComboBox<String>(trackPatterns).apply { isEditable = true; editor.textProperty().bindBidirectional(TRACKNAMEPATTERN) },
				1, 0)
		patternPane.add(patternLabel(TRACKNAMEPATTERN,
				Track(title = "Bring The Madness (feat. Mayor Apeshit) (Aero Chord Remix)", artistsTitle = "Excision & Pegboard Nerds", artists = listOf(Artist("Pegboard Nerds"), Artist("Excision")))),
				1, 1)
		patternPane.add(Label("Album Tracks pattern"),
				0, 2, 1, 2)
		patternPane.add(ComboBox<String>(albumTrackPatterns).apply { isEditable = true; editor.textProperty().bindBidirectional(ALBUMTRACKNAMEPATTERN) },
				1, 2)
		patternPane.add(patternLabel(ALBUMTRACKNAMEPATTERN,
				ReleaseFile("Gareth Emery & Standerwick - Saving Light (The Remixes) [feat. HALIENE] - 3 Saving Light (INTERCOM Remix) [feat. HALIENE]", false)),
				1, 3)
		add(patternPane)
		
		// Apply filters
		add(searchField)
		releaseView.predicate.bind({
			if (releaseSearch.predicate == alwaysTruePredicate && searchField.text.isEmpty()) null
			else { parent, value -> parent != releaseView.root && value.toString().contains(searchField.text, true) && releaseSearch.predicate.test(value) }
		}, searchField.textProperty(), releaseSearch.predicateProperty)
		trackView.root.bindPredicate(searchField.textProperty())
		searchField.textProperty().addListener { _ ->
			releaseView.root.children.forEach { (it as CheckBoxTreeItem).updateSelection() }
			trackView.root.updateSelection()
		}
		
		// add Views
		val pane = gridPane()
		pane.add(HBox(5.0, Label("Releasedate").grow(Priority.NEVER), releaseSearch.conditionBox, releaseSearch.searchField), 0, 0)
		pane.add(releaseView, 0, 1)
		pane.add(trackView, 1, 0, 1, 2)
		pane.maxWidth = Double.MAX_VALUE
		pane.children.forEach { GridPane.setHgrow(it, Priority.ALWAYS) }
		fill(pane)
		
		// Qualities
		val buttons = ImmutableObservableList(*qualities.map {
			ToggleButton(it.replace('_', ' ').toUpperCase()).apply {
				userData = it
				isSelected = userData == QUALITY()
			}
		}.toTypedArray())
		add(SegmentedButton(buttons)).toggleGroup.selectedToggleProperty().listen { if (it != null) QUALITY.put(it.userData) }
		QUALITY.listen { buttons.forEach { button -> button.isSelected = button.userData == it } }
		
		// Misc options
		addRow(/* todo hide downloaded
                 CheckBox("Hide songs I have already downloaded").bind(HIDEDOWNLOADED)
                .tooltip("Only works if the Patterns and Folders are correctly set"),*/
				createButton("Select all Songs (DO NOT click this if you are not connected to the internet!)") {
					trackView.checkModel.clearChecks()
					val albums = releaseView.get("Album")
					albums.isSelected = true
					ProgressDialog(SimpleTask("Updating", "Fetching") {
						val deferred = albums.internalChildren.map {
							async {
								(APIConnection("catalog", "release", it.value.id, "tracks").getTracks()
										?: throw Exception("No connection!")).map { it.toString().trim().toLowerCase() }
							}
						}
						val allAlbumTracks = HashSet<String>()
						var progress = 0
						val max = deferred.size
						deferred.forEach {
							allAlbumTracks.addAll(it.await())
							updateProgress(progress++, max)
						}
						onJFX {
							trackView.root.internalChildren.forEach {
								(it as CheckBoxTreeItem).isSelected = it.normalisedValue !in allAlbumTracks
							}
						}
					}).show()
				}.tooltip("Selects all Albums and then all Tracks that are not included in these"))
		addRow(TextField().apply {
			promptText = "connect.sid"
			// todo better instructions
			tooltip = Tooltip("Log into monstercat.com from your browser, find the cookie \"connect.sid\" from \"connect.monstercat.com\" and copy the content into here (which usually starts with \"s%3A\")")
			textProperty().bindBidirectional(CONNECTSID)
			maxWidth = Double.MAX_VALUE
		}.grow(), Button("Start Download").apply {
			arrayOf<Observable>(patternValid, noItemsSelected, CONNECTSID, QUALITY).addListener {
				refreshDownloadButton(this)
			}
			refreshDownloadButton(this)
		})
		
	}
	
	private fun refreshDownloadButton(button: Button) {
		button.text = "Checking..."
		launch {
			var valid = false
			val text = when (APIConnection.checkCookie()) {
				CookieValidity.NOCONNECTION -> "No connection"
				CookieValidity.NOUSER -> "Invalid connect.sid"
				CookieValidity.NOGOLD -> "Account is not subscribed to Gold"
				CookieValidity.GOLD -> when {
					!patternValid.value -> "Invalid pattern"
					QUALITY().isEmpty() -> "No format selected"
					noItemsSelected.value -> "Nothing selected to download"
					else -> {
						valid = true
						"Start Download"
					}
				}
			}
			onJFX {
				button.text = text
				if (valid)
					button.setOnAction { startDownload() }
				else
					button.setOnAction { refreshDownloadButton(button) }
			}
		}
	}
	
	private fun startDownload() {
		children.clear()
		releaseView.clearPredicate()
		trackView.clearPredicate()
		
		val cache = Cache<String, Image>()
		val taskView = TaskProgressView<Downloader>()
		taskView.setGraphicFactory {
			ImageView(cache.getOrPut(it.coverUrl) {
				val url = "$it?image_width=64".replace(" ", "%20")
				Image(HttpClientBuilder.create().build().execute(HttpGet(url)).entity.content)
			})
		}
		
		val context = Executors.newCachedThreadPool().asCoroutineDispatcher()
		val job = launch {
			val releases: List<MusicResponse> = releaseView.checkedItems.filter { it.isLeaf }.map { it.value }
			val tracks: List<MusicResponse> = trackView.checkedItems.filter { it.isLeaf }.map { it.value }
			logger.fine("Starting Downloader for ${releases.size} Releases and ${tracks.size} Tracks")
			for (item in tracks + releases) {
				val task = item.downloader()
				var added = false
				onJFX { taskView.tasks.add(task); added = true }
				task.launch(context)
				while (!added || taskView.tasks.size >= DOWNLOADTHREADS())
					delay(200)
			}
		}
		
		val cancelButton = Button("Finish").onClick { this.disableProperty().set(true); job.cancel() }
		cancelButton.maxWidth = Double.MAX_VALUE
		add(cancelButton)
		
		val threadSpinner = intSpinner(0, 5, initial = DOWNLOADTHREADS())
		DOWNLOADTHREADS.bind(threadSpinner.valueProperty())
		addLabeled("Download Threads", threadSpinner)
		// todo progress indicator
		fill(taskView)
		taskView.tasks.addListener(ListChangeListener {
			if (it.list.isEmpty())
				cancelButton.apply {
					setOnMouseClicked { initialize() }
					text = "Back"
					isDisable = false
				}
		})
	}
	
}

class TrackView : FilterableCheckTreeView<Track>(Track(title = "Tracks")) {
	init {
		setOnMouseClicked {
			if (it.clickCount == 2) {
				val selected = selectionModel.selectedItem ?: return@setOnMouseClicked
				if (selected.isLeaf)
					Player.playTrack(selected.value)
			}
		}
		root.value = Track(title = "Loading Tracks...")
		launch {
			Tracks.tracks?.sortedBy { it.toString() }?.forEach {
				root.internalChildren.add(FilterableTreeItem(it))
			}
			onJFX {
				root.value = Track(title = "Tracks")
			}
		}
	}
}

class ReleaseView : FilterableCheckTreeView<Release>(Release(title = "Releases")) {
	
	val categories = arrayOf("Single", "Album", "Monstercat Collection", "Best of", "Podcast", "Mixes")
	
	init {
		setOnMouseClicked {
			if (it.clickCount == 2) {
				val selected = selectionModel.selectedItem ?: return@setOnMouseClicked
				if (selected.isLeaf)
					Player.play(selected.value)
			}
		}
		launch {
			val roots = categories.associate { Pair(it, FilterableTreeItem(Release(title = it))) }
			Releases.getReleases().forEach {
				roots[it.type]?.internalChildren?.add(FilterableTreeItem(it))
						?: logger.warning("Unknown Release type: ${it.type}")
			}
			onJFX { root.internalChildren.addAll(roots.values) }
		}
	}
	
	fun get(name: String) = root.internalChildren[categories.indexOf(name)] as FilterableTreeItem
	
}

open class FilterableCheckTreeView<T : Any>(rootValue: T) : CheckTreeView<T>() {
	val root = FilterableTreeItem(rootValue)
	val checkedItems: ObservableList<TreeItem<T>>
		get() = checkModel.checkedItems
	val predicate
		get() = root.predicateProperty()
	
	init {
		root.isExpanded = true
		setRoot(root)
	}
	
	fun clearPredicate() {
		predicate.unbind()
		predicate.set { _, _ -> true }
	}
}

fun CheckBoxTreeItem<*>.updateSelection() {
	children.firstOrNull()?.run {
		val child = this as CheckBoxTreeItem
		child.isSelected = !child.isSelected
		child.isSelected = !child.isSelected
	} ?: run { isIndeterminate = true }
}

/*
private fun downloaded(code: Int, name: Any, additional: String? = null) {
	downloadedCount++
	if (code < downloaded.size)
		downloaded[code] = downloaded[code] + 1
	var message = messages[code] + " " + name
	if (additional != null)
		message += ": " + additional
	log(message)
	if (code < downloaded.size)
		updateStatus("Done: %s / %s", downloadedCount, releases.size)
	if (downloaded[0] > 0) {
		val avgTime = (System.currentTimeMillis() - startTime) / downloaded[0]
		estimatedTime.text = "Estimated time left: " + format.format(Date(avgTime * (releases.size - downloadedCount)))
	}
}
*/


/*override fun init() {
	if () {
		val alert = Alert(Alert.AlertType.CONFIRMATION, "The last Download ended unfinished. Do you want to continue it now?", ButtonType.YES, ButtonType.NO, ButtonType("Ask later"))
		when (alert.showAndWait().getOrNull()) {
			ButtonType.YES -> {
				monsterUtilities.tabPane.selectionModel.select(monsterUtilities.tabs.indexOf(this))
				startDownload()
			}
			ButtonType.NO -> { reset }
		}
	}
}*/
