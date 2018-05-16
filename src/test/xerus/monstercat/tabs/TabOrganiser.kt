package xerus.monstercat.tabs

/*
class TabOrganiser : VBox(), BaseTab {

    private val downloadPath = DOWNLOADDIR()
    private val pane = GridPane()
    private val options = ArrayList<Option>()
    private val quality: ObservableValue<Int>

    private var row: Int = 0

    init {
        Option("ekstatis", "Rename & change title of Droptek - Ekstatis to Ekstasis",
                downloadPath.resolve("Droptek - Fragments EP").resolve(ReleaseFile("Droptek - Fragments EP - 4 Ekstatis", false).toFileName() + "." + QUALITY.get().split(" ").first().toLowerCase()).toString())
        Option("tuttutcover", "Correct the cover of Tut tut Child - Come to the End; then stop",
                downloadPath.resolve("Tut Tut Child - Come to the End; Then Stop").toString())
        Option("genres", "Apply MCatalog Genres to the Tags")
        Option("albumCovers", "Remove covers from Album tracks and instead put the cover into the folder")
        Option("covers", "Replace/Add covers with specified quality")

        val qual = Spinner(object : SpinnerValueFactory<Int>() {
            init {
                value = 512
            }

            override fun decrement(steps: Int) {
                value = if (value == 3000)
                    4096 shr steps
                else
                    value shr steps
                if (value < 1)
                    value = 1
            }

            override fun increment(steps: Int) {
                value = value shl steps
                if (value > 2048)
                    value = 3000
            }
        })
        qual.isEditable = false
        qual.tooltip = Tooltip("Note: 3000 actually means maximal available quality, but that's 3000 most of the time")
        quality = qual.valueProperty()
        val box = HBox(Label("Cover resolution:  "), qual)
        box.alignment = Pos.CENTER_LEFT
        addRow(box)

        val start = Button("Do it!")
        start.setOnAction {
            options.filter { it.isSelected }
                    .forEach {
                        val method = it.name
                        try {
                            if (it.textField == null)
                                javaClass.getMethod(method).invoke(this)
                            else
                                javaClass.getMethod(method, String::class.java).invoke(this, it.textField.text)
                        } catch (e: Exception) {
                            throw RuntimeException("Method $method not found: $e")
                        }
                    }
        }
        addRow(start)

        pane.hgap = 5.0
        children.add(pane)
    }

    private fun addRow(vararg nodes: Node) {
        pane.addRow(row++, *nodes)
        pane.rowConstraints.add(RowConstraints(25.0, 32.0, 40.0, Priority.SOMETIMES, VPos.CENTER, false))
    }

    private fun ekstatis(path: String) {

    }

    private fun tuttutcover(path: String) {

    }

    private fun genres() {

    }

    private fun albumCovers() {

    }

    private fun covers() {

    }

    private inner class Option constructor(val name: String, desc: String, text: String? = null) : CheckBox(desc) {

        val textField: TextField? = if (text != null) TextField(text) else null

        internal val nodes: Array<Control> = if (textField == null) arrayOf(this) else arrayOf(this, textField)

        init {
            addRow(*nodes)
        }
    }

    */
/*void newPane(String name) {
		row = 0;
		curPane = new GridPane();
		accordion.getPanes().add(new TitledPane(name, curPane));
	}*//*


}
*/
