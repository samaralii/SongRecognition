package edu.cmu.pocketsphinx.demo.utils

import android.content.Context
import android.view.View
import android.widget.Toast

const val BROADCAST_ACTION = "edu.cmu.pocketsphinx.demo"


fun View.hide() {
    this.visibility = View.INVISIBLE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}