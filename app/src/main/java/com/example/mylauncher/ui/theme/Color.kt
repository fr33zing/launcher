package com.example.mylauncher.ui.theme

import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

class Catppuccin {
    companion object {
        val Latte = LattePalette()
        val Frappe = FrappePalette()
        val Macchiato = MacchiatoPalette()
        val Mocha = MochaPalette()

        var Current: Palette = Frappe
    }

    abstract class Palette {
        companion object {}

        abstract val rosewater: Color
        abstract val flamingo: Color
        abstract val pink: Color
        abstract val mauve: Color
        abstract val red: Color
        abstract val maroon: Color
        abstract val peach: Color
        abstract val yellow: Color
        abstract val green: Color
        abstract val teal: Color
        abstract val sky: Color
        abstract val sapphire: Color
        abstract val blue: Color
        abstract val lavender: Color
        abstract val text: Color
        abstract val subtext1: Color
        abstract val subtext0: Color
        abstract val overlay2: Color
        abstract val overlay1: Color
        abstract val overlay0: Color
        abstract val surface2: Color
        abstract val surface1: Color
        abstract val surface0: Color
        abstract val base: Color
        abstract val mantle: Color
        abstract val crust: Color
    }

    class LattePalette : Palette() {
        override val rosewater = Color(0xFFdc8a78)
        override val flamingo = Color(0xFFdd7878)
        override val pink = Color(0xFFea76cb)
        override val mauve = Color(0xFF8839ef)
        override val red = Color(0xFFd20f39)
        override val maroon = Color(0xFFe64553)
        override val peach = Color(0xFFfe640b)
        override val yellow = Color(0xFFdf8e1d)
        override val green = Color(0xFF40a02b)
        override val teal = Color(0xFF179299)
        override val sky = Color(0xFF04a5e5)
        override val sapphire = Color(0xFF209fb5)
        override val blue = Color(0xFF1e66f5)
        override val lavender = Color(0xFF7287fd)
        override val text = Color(0xFF4c4f69)
        override val subtext1 = Color(0xFF5c5f77)
        override val subtext0 = Color(0xFF6c6f85)
        override val overlay2 = Color(0xFF7c7f93)
        override val overlay1 = Color(0xFF8c8fa1)
        override val overlay0 = Color(0xFF9ca0b0)
        override val surface2 = Color(0xFFacb0be)
        override val surface1 = Color(0xFFbcc0cc)
        override val surface0 = Color(0xFFccd0da)
        override val crust = Color(0xFFdce0e8)
        override val mantle = Color(0xFFe6e9ef)
        override val base = Color(0xFFeff1f5)
    }

    class FrappePalette : Palette() {
        override val rosewater = Color(0xFFf2d5cf)
        override val flamingo = Color(0xFFeebebe)
        override val pink = Color(0xFFf4b8e4)
        override val mauve = Color(0xFFca9ee6)
        override val red = Color(0xFFe78284)
        override val maroon = Color(0xFFea999c)
        override val peach = Color(0xFFef9f76)
        override val yellow = Color(0xFFe5c890)
        override val green = Color(0xFFa6d189)
        override val teal = Color(0xFF81c8be)
        override val sky = Color(0xFF99d1db)
        override val sapphire = Color(0xFF85c1dc)
        override val blue = Color(0xFF8caaee)
        override val lavender = Color(0xFFbabbf1)
        override val text = Color(0xFFc6d0f5)
        override val subtext1 = Color(0xFFb5bfe2)
        override val subtext0 = Color(0xFFa5adce)
        override val overlay2 = Color(0xFF949cbb)
        override val overlay1 = Color(0xFF838ba7)
        override val overlay0 = Color(0xFF737994)
        override val surface2 = Color(0xFF626880)
        override val surface1 = Color(0xFF51576d)
        override val surface0 = Color(0xFF414559)
        override val base = Color(0xFF303446)
        override val mantle = Color(0xFF292c3c)
        override val crust = Color(0xFF232634)
    }

    class MacchiatoPalette : Palette() {
        override val rosewater = Color(0xFFf4dbd6)
        override val flamingo = Color(0xFFf0c6c6)
        override val pink = Color(0xFFf5bde6)
        override val mauve = Color(0xFFc6a0f6)
        override val red = Color(0xFFed8796)
        override val maroon = Color(0xFFee99a0)
        override val peach = Color(0xFFf5a97f)
        override val yellow = Color(0xFFeed49f)
        override val green = Color(0xFFa6da95)
        override val teal = Color(0xFF8bd5ca)
        override val sky = Color(0xFF91d7e3)
        override val sapphire = Color(0xFF7dc4e4)
        override val blue = Color(0xFF8aadf4)
        override val lavender = Color(0xFFb7bdf8)
        override val text = Color(0xFFcad3f5)
        override val subtext1 = Color(0xFFb8c0e0)
        override val subtext0 = Color(0xFFa5adcb)
        override val overlay2 = Color(0xFF939ab7)
        override val overlay1 = Color(0xFF8087a2)
        override val overlay0 = Color(0xFF6e738d)
        override val surface2 = Color(0xFF5b6078)
        override val surface1 = Color(0xFF494d64)
        override val surface0 = Color(0xFF363a4f)
        override val base = Color(0xFF24273a)
        override val mantle = Color(0xFF1e2030)
        override val crust = Color(0xFF181926)
    }

    class MochaPalette : Palette() {
        override val rosewater = Color(0xFFf5e0dc)
        override val flamingo = Color(0xFFf2cdcd)
        override val pink = Color(0xFFf5c2e7)
        override val mauve = Color(0xFFcba6f7)
        override val red = Color(0xFFf38ba8)
        override val maroon = Color(0xFFeba0ac)
        override val peach = Color(0xFFfab387)
        override val yellow = Color(0xFFf9e2af)
        override val green = Color(0xFFa6e3a1)
        override val teal = Color(0xFF94e2d5)
        override val sky = Color(0xFF89dceb)
        override val sapphire = Color(0xFF74c7ec)
        override val blue = Color(0xFF89b4fa)
        override val lavender = Color(0xFFb4befe)
        override val text = Color(0xFFcdd6f4)
        override val subtext1 = Color(0xFFbac2de)
        override val subtext0 = Color(0xFFa6adc8)
        override val overlay2 = Color(0xFF9399b2)
        override val overlay1 = Color(0xFF7f849c)
        override val overlay0 = Color(0xFF6c7086)
        override val surface2 = Color(0xFF585b70)
        override val surface1 = Color(0xFF45475a)
        override val surface0 = Color(0xFF313244)
        override val base = Color(0xFF1e1e2e)
        override val mantle = Color(0xFF181825)
        override val crust = Color(0xFF11111b)
    }
}

