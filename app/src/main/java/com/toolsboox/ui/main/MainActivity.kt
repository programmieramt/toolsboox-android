package com.toolsboox.ui.main

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.databinding.ActivityMainBinding
import com.toolsboox.databinding.ToolbarBinding
import com.toolsboox.di.MainSharedPreferencesModule
import com.toolsboox.ui.BaseActivity
import com.toolsboox.utils.ReleaseTree
import dagger.hilt.android.AndroidEntryPoint
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * A dashboard screen that offers the main menu.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */

@AndroidEntryPoint
class MainActivity : BaseActivity<MainPresenter>(), MainView {

    /**
     * The view model.
     */
    private val viewModel by viewModels<MainViewModel>()

    /**
     * The injected shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The view binding.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * OnCreate hook.
     *
     * @param savedInstanceState the saved state of the instance
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        setSupportActionBar(binding.mainToolbar.root)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        // Temporary (?) fix for https://github.com/gaborauth/toolsboox-android/issues/305
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }

        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        sharedPreferences.edit().putString("androidId", androidId).apply()

        val headerAndroidId = binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.navigation_header_android_id)
        headerAndroidId.text = androidId

        val headerVersion = binding.navigationView.getHeaderView(0)
            .findViewById<TextView>(R.id.navigation_header_version)
        headerVersion.text = getString(R.string.main_version)
            .format(BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)

        val preferences = MainSharedPreferencesModule.provideSharedPreferences(this)
        preferences.edit().putLong("lastTimestamp", Date().time).apply()

        // One-time migration: cancel the legacy cloud-sync periodic work, removed with the cloud sync feature
        WorkManager.getInstance(this).cancelUniqueWork("calendar-cloud-sync")

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_item_calendar -> {
                    val calendarStartActionId = when (sharedPreferences.getInt("calendarStartView", 0)) {
                        0 -> R.id.action_to_calendar_day
                        1 -> R.id.action_to_calendar_month
                        2 -> R.id.action_to_calendar_quarter
                        3 -> R.id.action_to_calendar_year
                        else -> R.id.action_to_calendar_day
                    }
                    binding.fragmentContent.findNavController().navigate(calendarStartActionId, bundleOf())
                }
            }

            menuItem.isChecked = true
            binding.drawerLayout.closeDrawers()
            true
        }

        binding.mainToolbar.toolbarBack.setOnClickListener {
            onBackPressed()
        }

        presenter.onViewCreated()
    }

    /**
     * Activity onResume.
     */
    override fun onResume() {
        super.onResume()

        val host = intent?.data?.host
        val path = intent?.data?.path
        if (host == "app") {
            if (path?.startsWith("/calendar") == true) {
                val defaultCalendarStartActionId = when (sharedPreferences.getInt("calendarStartView", 0)) {
                    0 -> R.id.action_to_calendar_day
                    1 -> R.id.action_to_calendar_month
                    2 -> R.id.action_to_calendar_quarter
                    3 -> R.id.action_to_calendar_year
                    else -> R.id.action_to_calendar_day
                }

                val calendarStartActionId = when (path) {
                    "/calendar/day" -> R.id.action_to_calendar_day
                    "/calendar/month" -> R.id.action_to_calendar_month
                    "/calendar/quarter" -> R.id.action_to_calendar_quarter
                    "/calendar/year" -> R.id.action_to_calendar_year
                    else -> defaultCalendarStartActionId
                }

                val bundle = Bundle()
                binding.fragmentContent.findNavController().navigate(calendarStartActionId, bundle)
            }
        }
    }

    /**
     * Get the main toolbar.
     *
     * @return the toolbar
     */
    fun getToolbar(): ToolbarBinding = binding.mainToolbar

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    override fun showError(t: Throwable?, @StringRes errorResId: Int) {
        t?.let { Timber.e(it, getString(errorResId)) }
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    override fun showMessage(@StringRes messageResId: Int) {
        Snackbar.make(binding.mainToolbar.root, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show progress and hide login form.
     */
    override fun showLoading() {
    }

    /**
     * Hide progress and show login form.
     */
    override fun hideLoading() {
    }

    /**
     * Instantiate the presenter.
     */
    override fun presenter(): MainPresenter {
        return MainPresenter(this)
    }

    /**
     * Close the drawer menu.
     *
     * @param item the selected menu item
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * OnBackPressed hook.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            orientateFragment(null)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Orientate the fragment by name.
     *
     * @param fragment the fragment
     */
    private fun orientateFragment(fragment: Fragment?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        Timber.i("Sensor portrait: ${fragment?.javaClass?.name}")
    }
}
