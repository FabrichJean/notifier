package com.notifier.client.ui

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.notifier.client.R
import com.notifier.client.databinding.ActivityMainBinding

private enum class Tab(val index: Int) { DEVICES(0), NOTIFICATIONS(1), SETTINGS(2) }

class MainActivity : AppCompatActivity(), TabHost {

    private lateinit var binding: ActivityMainBinding
    private var currentTab: Tab = Tab.DEVICES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()
        useLightSystemBarIcons()

        binding.bottomNav.navDevicesTab.setOnClickListener { switchTab(Tab.DEVICES) }
        binding.bottomNav.navNotificationsTab.setOnClickListener { switchTab(Tab.NOTIFICATIONS) }
        binding.bottomNav.navNotificationsCircle.setOnClickListener { switchTab(Tab.NOTIFICATIONS) }
        binding.bottomNav.navSettingsTab.setOnClickListener { switchTab(Tab.SETTINGS) }

        binding.bottomNav.navNotificationsIcon.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dash_emerald_bright))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, DevicesFragment())
                .commit()
        }
        updateTabIndicators()
    }

    override fun showNotificationsTab(deviceId: String?, deviceName: String?) {
        switchTab(Tab.NOTIFICATIONS, NotificationsFragment.newInstance(deviceId, deviceName))
    }

    private fun switchTab(target: Tab, explicitFragment: Fragment? = null) {
        val isRedundantTap = target == currentTab && explicitFragment == null && target != Tab.NOTIFICATIONS
        if (isRedundantTap) return

        val forward = target.index > currentTab.index
        val fragment = explicitFragment ?: when (target) {
            Tab.DEVICES -> DevicesFragment()
            Tab.NOTIFICATIONS -> NotificationsFragment.newInstance()
            Tab.SETTINGS -> ServerSettingsFragment()
        }

        val (enterAnim, exitAnim) = if (forward) {
            R.anim.slide_in_right to R.anim.slide_out_left
        } else {
            R.anim.slide_in_left to R.anim.slide_out_right
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim)
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        currentTab = target
        updateTabIndicators()
    }

    private fun updateTabIndicators() {
        setBottomNavTabActive(
            binding.bottomNav.navDevicesIndicator, binding.bottomNav.navDevicesIcon, binding.bottomNav.navDevicesLabel,
            active = currentTab == Tab.DEVICES
        )
        val notificationsColorRes = if (currentTab == Tab.NOTIFICATIONS) R.color.dash_emerald_bright else R.color.glass_text_secondary
        binding.bottomNav.navNotificationsLabel.setTextColor(ContextCompat.getColor(this, notificationsColorRes))
        setBottomNavTabActive(
            binding.bottomNav.navSettingsIndicator, binding.bottomNav.navSettingsIcon, binding.bottomNav.navSettingsLabel,
            active = currentTab == Tab.SETTINGS
        )
    }
}
