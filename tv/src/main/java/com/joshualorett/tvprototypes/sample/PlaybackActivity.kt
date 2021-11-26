package com.joshualorett.tvprototypes.sample

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, PlaybackVideoFragment())
                    .commit()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        val userInteractionListener = supportFragmentManager
            .findFragmentById(android.R.id.content) as UserInteractionListener?
        userInteractionListener?.onUserInteraction()
    }
}