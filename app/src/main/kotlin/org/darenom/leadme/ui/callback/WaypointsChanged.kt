package org.darenom.leadme.ui.callback

/**
 * Created by admadmin on 16/03/2018.
 */
interface WaypointsChanged {
    fun onListchanged(listaddr: java.util.ArrayList<String>, listposs: java.util.ArrayList<String>)
}