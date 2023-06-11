package mo.browser.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewFeature
import mo.browser.app.databinding.FragmentFirstBinding
import mo.browser.app.module.DataUsageEntry
import mo.browser.app.module.PreferenceDatabase
import java.io.File
import java.text.DecimalFormat

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    lateinit var webView: WebView

    private lateinit var customSearchView: AutoCompleteTextView
    private lateinit var swipe_refresh_layout: SwipeRefreshLayout
    private lateinit var settings_menu: ImageView

    private lateinit var preferenceManager: PreferenceDatabase
    private var currentSearchEngine = ""
    private var currentSearchQuery = ""

    private val homeTabUrl =
        "file:///android_asset/home.html" // Replace with your home tab HTML file or URL

    private val dataUsageList = mutableListOf<DataUsageEntry>()
    private val browsingHistory = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceDatabase(requireContext())
        currentSearchEngine = preferenceManager.getDefaultSearchEngine()

        swipe_refresh_layout = view.findViewById(R.id.swipe_refresh_layout)
        customSearchView = view.findViewById(R.id.custom_search_view)
        settings_menu = view.findViewById(R.id.settings_menu)

        val inputMethodManager = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        customSearchView.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        val filters = arrayOf<InputFilter>(object : InputFilter {
            override fun filter(
                source: CharSequence?,
                start: Int,
                end: Int,
                dest: Spanned?,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                return source?.toString()?.replace(" ", "")
            }
        })
        customSearchView.filters = filters

        customSearchView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = customSearchView.text.trim().toString()
                loadUrlWithSearchEngine(query)
                customSearchView.clearFocus()
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                true
            } else {
                false
            }
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            browsingHistory
        )
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        customSearchView.setAdapter(adapter)
        customSearchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val enteredText = s.toString()
                val filteredSuggestions =
                    browsingHistory.filter { it.startsWith(enteredText, ignoreCase = true) }
                adapter.clear()
                adapter.addAll(filteredSuggestions)
                adapter.notifyDataSetChanged()
            }
        })

        swipe_refresh_layout.setOnRefreshListener {
            webView.reload()
        }

        customSearchView.setOnClickListener {
            if (customSearchView.editableText.isNotEmpty()) {
                customSearchView.selectAll()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }


        settings_menu.setOnClickListener { views ->
            val popupMenu = PopupMenu(requireContext(), views)
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.custom_menu, popupMenu.menu)
            // Set listeners for menu item clicks
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.settings_menu -> {
                        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                        true
                    }

                    else -> false
                }
            }

            // Show the popup menu
            popupMenu.show()
        }

        webView = binding.webview
        setupWebView()
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.O_MR1)
            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
                    callback!!.backToSafety(true)
                    Toast.makeText(view!!.context, "Unsafe web page blocked.", Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (url?.equals(homeTabUrl) == true) {
                    customSearchView.setText("")
                    customSearchView.hint = "Search or type web address"
                } else {
                    currentSearchQuery = url.toString()
                    customSearchView.setText(url)
                }
                if (swipe_refresh_layout != null) {
                    swipe_refresh_layout.isRefreshing = false
                }

                if (url == homeTabUrl) {
                    customSearchView.setText("")
                    customSearchView.hint = "Search or type web address"
                } else {
                    customSearchView.setText(url)
                }

                if (url != null) {
                    if (!browsingHistory.contains(url)) {
                        browsingHistory.add(url)
                    }
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url == homeTabUrl) {
                    customSearchView.setText("")
                    customSearchView.hint = "Search or type web address"
                } else {
                    customSearchView.setText(url)
                }

                val lockIcon = if (url?.startsWith("https://") == true) {
                    R.drawable.round_lock
                } else {
                    R.drawable.round_unlocked
                }
                customSearchView.setCompoundDrawablesWithIntrinsicBounds(lockIcon, 0, 0, 0)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                customSearchView.setOnClickListener {
                    customSearchView.selectAll()
                }
                if (url == homeTabUrl) {
                    customSearchView.setText("")
                    customSearchView.hint = "Search or type web address"
                } else {
                    customSearchView.setText(url)
                }
                view.loadUrl(url)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.allowFileSchemeCookies()
                return true
            }
        }

        val webSettings: WebSettings = webView.settings
        webSettings.loadsImagesAutomatically = true
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        val defaultUserAgent = webSettings.userAgentString
        val userAgent = StringBuilder(defaultUserAgent)
            .append(" ")
        userAgent.append("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Mobile Safari/537.36")
        userAgent.append("Mozilla/5.0 (iPhone; CPU iPhone OS 14_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Mobile/15E148 Safari/604.1")

        webSettings.userAgentString = userAgent.toString()
        webView.loadUrl(homeTabUrl)

        val cacheSize = getCacheSize()
        preferenceManager.setChacheSize(cacheSize)

        val currentDataUsage = getInternetDataUsage()
        preferenceManager.setCurrenDataUsage(currentDataUsage)
    }

    private fun loadUrlWithSearchEngine(query: String) {
        var url = ""

        // Check if the query starts with "http://" or "https://"
        if (query.startsWith("http://") || query.startsWith("https://")) {
            url = query
        }
        // Check if the query ends with a common image extension
        else if (query.matches(Regex(".*\\.(jpg|jpeg|png|gif)"))) {
            url = query
        }
        // Check if the query ends with a common video extension
        else if (query.matches(Regex(".*\\.(mp4|avi|mov|wmv)"))) {
            url = query
        }
        // Check if the query ends with a common file extension
        else if (query.matches(Regex(".*\\.(pdf|doc|docx|txt|xls|xlsx|ppt|pptx|csv|rtf|odt|ods|odp|odg|zip|rar|exe|dat|bat|jpg|jpeg|png|gif|bmp)"))) {
            url = query
        }
        // Check if the query ends with a web domain extension
        else if (query.matches(Regex(".*\\.[a-z]{2,}"))) {
            url = "https://$query"
        }
        // Treat the query as a search term
        else {
            url = "$currentSearchEngine$query"
        }

        if (url.isNotEmpty()) {
            webView.loadUrl(url)
            if (!browsingHistory.contains(url)) {
                browsingHistory.add(url)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("searchQuery", currentSearchQuery)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            currentSearchQuery = savedInstanceState.getString("searchQuery", "")
            customSearchView.setText(currentSearchQuery)
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentSearchQuery.isNotEmpty()) {
            loadUrlWithSearchEngine(currentSearchQuery)
        }
    }

    // Call this method to switch the search engine
    private fun switchSearchEngine(searchEngineUrl: String) {
        currentSearchEngine = searchEngineUrl
        webView.loadUrl(currentSearchEngine)
    }

    fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var fileSize = size.toDouble()
        var unitIndex = 0
        val formatter = DecimalFormat("0.##")

        while (fileSize >= 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }

        return formatter.format(fileSize) + " " + units[unitIndex]
    }

    // Get cache size
    private fun getCacheSize(): String {
        val webViewCacheDir = webView.context.cacheDir
        val cacheSize = getDirectorySize(webViewCacheDir)
        return formatFileSize(cacheSize)
    }

    // Recursive function to calculate directory size
    private fun getDirectorySize(directory: File): Long {
        var size: Long = 0
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    size += file.length()
                } else if (file.isDirectory) {
                    size += getDirectorySize(file)
                }
            }
        }

        return size
    }

    private fun getInternetDataUsage(): Long {
        val uid = android.os.Process.myUid()
        val rxBytes = TrafficStats.getUidRxBytes(uid)
        val txBytes = TrafficStats.getUidTxBytes(uid)
        return rxBytes + txBytes
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}