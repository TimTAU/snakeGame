package fhaachen.snakegame.helper

import android.content.Context
import fhaachen.snakegame.R
import fhaachen.snakegame.helper.SharedPreferencesHelper.getSharedPreference

object ScoreHelper {
    /**
     * Returns saved last score
     */
    fun getLastScore(context: Context): String {
        return getSharedPreference(context, R.string.save_lastscore, 0).toString()
    }

    /**
     * Returns saved highscore
     */
    fun getHighScore(context: Context): String {
        return getSharedPreference(context, R.string.save_highscore, 0).toString()
    }

    /**
     * Saves given score and updates highscore if required
     *
     * @param score of last game
     */
    fun saveScore(context: Context, score: Int) {
        SharedPreferencesHelper.setSharedPreference(context, R.string.save_lastscore, score)
        // Set new highscore if given score is bigger than saved highscore
        if (score > getSharedPreference(context, R.string.save_highscore, 0)) {
            SharedPreferencesHelper.setSharedPreference(context, R.string.save_highscore, score)
        }
    }
}