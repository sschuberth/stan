package dev.schuberth.stan.gui

import tornadofx.*

class MainApp : App(MainView::class)

class MainView : View() {
    override val root = hbox {
        label("Hello world")
    }
}
