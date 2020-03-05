package fhaachen.snakegame.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

object SharedPreferencesHelper {
    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    fun getSharedPreference(context: Context, resourceStringValue: Int, defaultValue: String): String {
        return getSharedPreferences(context).getString(context.applicationContext.getString(resourceStringValue), defaultValue)!!
    }

    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    fun getSharedPreference(context: Context, resourceStringValue: Int, defaultValue: Int): Int {
        return getSharedPreferences(context).getInt(context.applicationContext.getString(resourceStringValue), defaultValue)
    }

    /**
     * Puts the given key value pair as an [SharedPreferences]
     *
     * @param resourceStringValue Resource string value
     * @param value               String value to be matched
     */
    fun setSharedPreference(context: Context, resourceStringValue: Int, value: String) {
        val editor = getSharedPreferencesEditor(context)
        editor.putString(context.applicationContext.getString(resourceStringValue), value)
        editor.apply()
    }

    /**
     * Puts the given key value pair as an [SharedPreferences]
     *
     * @param resourceStringValue Resource string value
     * @param value               int value to be matched
     */
    fun setSharedPreference(context: Context, resourceStringValue: Int, value: Int) {
        val editor = getSharedPreferencesEditor(context)
        editor.putInt(context.applicationContext.getString(resourceStringValue), value)
        editor.apply()
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        val activity = context as AppCompatActivity
        return activity.getPreferences(Context.MODE_PRIVATE)
    }

    private fun getSharedPreferencesEditor(context: Context): SharedPreferences.Editor {
        return getSharedPreferences(context).edit()
    }
}