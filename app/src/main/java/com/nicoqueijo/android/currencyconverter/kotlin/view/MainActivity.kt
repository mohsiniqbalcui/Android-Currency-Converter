package com.nicoqueijo.android.currencyconverter.kotlin.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.kotlin.service.AlarmService
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hideKeyboard
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.isServiceRunning
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ActivityScoped


@AndroidEntryPoint
@ActivityScoped
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    val context = this@MainActivity

    private lateinit var drawer: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView
    private lateinit var closeAppToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        handleNavigation()
        startMyAppsService()

    }

    private fun startMyAppsService() {

        if (!isServiceRunning(context, AlarmService::class.java.name)) {

            val intent = Intent(context, AlarmService::class.java)
            applicationContext.startService(intent)
        }
    }

    @SuppressLint("ShowToast")
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawer = findViewById(R.id.nav_drawer)
        initListeners()
        drawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        navView = findViewById(R.id.nav_view)
        closeAppToast = Toast.makeText(this, R.string.tap_to_close, Toast.LENGTH_SHORT)
    }

    private fun handleNavigation() {
        navController = findNavController(R.id.content_frame)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            viewModel.activeFragment.postValue(destination.id)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            drawer.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.tips -> {
                    fireTipsDialog()
                    false
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun fireTipsDialog() {
        MaterialAlertDialogBuilder(this)
            .setView(R.layout.tips)
            .setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_background))
            .show()
    }

    private fun initListeners() {
        actionBarDrawerToggle = object : ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.nav_drawer_open, R.string.nav_drawer_close
        ) {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                hideKeyboard()
            }
        }
    }

    private fun showNoInternetSnackbar() {
        Snackbar.make(findViewById(R.id.content_frame), R.string.no_internet, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (viewModel.activeFragment.value == R.id.selectorFragment) {
            navController.popBackStack()
        } else if (closeAppToast?.view?.isShown?.not() == true) {
            closeAppToast?.show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
//        billingProcessor.release()
        super.onDestroy()
    }
}
