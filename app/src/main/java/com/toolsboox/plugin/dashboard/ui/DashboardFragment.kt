package com.toolsboox.plugin.dashboard.ui

import android.content.Context
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.InputDevice
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.da.SquareItem
import com.toolsboox.databinding.FragmentDashboardBinding
import com.toolsboox.ot.SquareItemAdapter
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class DashboardFragment @Inject constructor() : ScreenFragment() {

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var presenter: DashboardPresenter

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_dashboard

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentDashboardBinding

    /**
     * The dashboard item adapter.
     */
    private lateinit var adapter: SquareItemAdapter


    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDashboardBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@DashboardFragment.requireContext(), 4)
        }

        val calendarStartActionId = when (sharedPreferences.getInt("calendarStartView", 0)) {
            0 -> R.id.action_to_calendar_day
            1 -> R.id.action_to_calendar_month
            2 -> R.id.action_to_calendar_quarter
            3 -> R.id.action_to_calendar_year
            else -> R.id.action_to_calendar_day
        }

        val androidId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        sharedPreferences.edit().putString("androidId", androidId).apply()
        Timber.i("Stored androidId: $androidId")

        val squareItems = mutableListOf<SquareItem>()
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_calendar_title), R.drawable.ic_dashboard_item_calendar,
                calendarStartActionId, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_sofort_title), R.drawable.ic_dashboard_item_sofort,
                R.id.action_to_sofort_list, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_oneonone_title), R.drawable.ic_dashboard_item_oneonone,
                R.id.action_to_oneonone_list, bundleOf()
            )
        )

        val clickListener = object : SquareItemAdapter.OnItemClickListener {
            override fun onItemClicked(squareItem: SquareItem) {
                Timber.i("Route to ${squareItem.title}")
                findNavController().navigate(squareItem.actionId, squareItem.bundle)
            }
        }

        adapter = SquareItemAdapter(this.requireContext(), squareItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        val inputManager = requireContext().getSystemService(Context.INPUT_SERVICE) as InputManager?
        val inputs = inputManager!!.inputDeviceIds
        for (i in inputs.indices) {
            val inputDevice = inputManager.getInputDevice(inputs[i])
            if (inputDevice?.supportsSource(InputDevice.SOURCE_STYLUS) == true) {
                Timber.e("Input %s supports stylus input", inputDevice.name)
            }
        }

        binding.buttonToggleAd.setOnClickListener {
            if (sharedPreferences.getBoolean("advertisements", true)) {
                sharedPreferences.edit().putBoolean("advertisements", false).apply()
            } else {
                sharedPreferences.edit().putBoolean("advertisements", true).apply()
            }

            updateAdButton()
        }

    }

    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.dashboard_title))

        askAppPermissions()
        deviceCheck()

        updateAdButton()
    }

    /**
     * Render the result of brand mismatch dialog.
     */
    private fun deviceCheck() {
        val brand = Build.BRAND.lowercase().contains("onyx")
        val device = Build.DEVICE.lowercase().contains("onyx")
        val manufacturer = Build.MANUFACTURER.lowercase().contains("onyx")
        val notifiedAboutDeviceMismatch = sharedPreferences.getBoolean("notifiedAboutDeviceMismatch", false)
        if (brand || device || manufacturer || notifiedAboutDeviceMismatch) return

        val message = getString(R.string.dashboard_device_mismatch_message).format(Build.BRAND, Build.DEVICE)

        sharedPreferences.edit().putBoolean("notifiedAboutDeviceMismatch", true).apply()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(R.string.dashboard_device_mismatch_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.cancel()
            }
        builder.create().show()
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }

    /**
     * Update the state of the advertisement enable-disable button.
     */
    private fun updateAdButton() {
    }
}
