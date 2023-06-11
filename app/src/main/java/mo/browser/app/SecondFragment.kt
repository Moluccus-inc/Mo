package mo.browser.app

import android.os.Bundle
import android.text.format.Formatter.formatFileSize
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import mo.browser.app.databinding.FragmentSecondBinding
import mo.browser.app.module.DataUsageEntry
import mo.browser.app.module.PreferenceDatabase
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale

class SecondFragment : Fragment() {
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: PreferenceDatabase
    private lateinit var searchEngineSpinner: AppCompatSpinner
    private val dataUsageList = mutableListOf<DataUsageEntry>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).title = "Mo Browser Settings"
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // Enable the home button
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.round_arrow_back)
        }

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        preferenceManager = PreferenceDatabase(requireContext())
        searchEngineSpinner = binding.searchEngine
        val searchEngines = resources.getStringArray(R.array.search_engine)

        // Set the adapter for the spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, searchEngines)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item) // Set the custom layout for dropdown items
        searchEngineSpinner.adapter = adapter

        // Set the default selection based on stored preference
        val defaultSearchEngine = preferenceManager.getDefaultSearchEngine()
        val defaultSearchEngineIndex = searchEngines.indexOf(defaultSearchEngine)
        searchEngineSpinner.setSelection(defaultSearchEngineIndex)

        // Set the selected position based on stored preference
        val selectedPosition = preferenceManager.getSelectedSearchEnginePosition()
        searchEngineSpinner.setSelection(selectedPosition)

        // Set a listener for spinner selection
        searchEngineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSearchEngine = searchEngines[position]
                preferenceManager.setSelectedSearchEnginePosition(position)
                when (selectedSearchEngine.toLowerCase(Locale.ROOT)) {
                    "brave" -> {
                        preferenceManager.setDefaultSearchEngine("https://search.brave.com/search?q=")
                    }
                    "google" -> {
                        preferenceManager.setDefaultSearchEngine("https://www.google.com/search?q=")
                    }
                    "duckduckgo" -> {
                        preferenceManager.setDefaultSearchEngine("https://duckduckgo.com/?hps=1&q=")
                    }
                    "qwant" -> {
                        preferenceManager.setDefaultSearchEngine("https://www.qwant.com/?q=")
                    }
                    "bing" -> {
                        preferenceManager.setDefaultSearchEngine("https://www.bing.com/search?q=")
                    }
                    "startpage" -> {
                        preferenceManager.setDefaultSearchEngine("https://www.startpage.com/sp/search")
                    }
                    "ecosia" -> {
                        preferenceManager.setDefaultSearchEngine("https://www.ecosia.org/search?method=index&q=")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                preferenceManager.setDefaultSearchEngine("https://search.brave.com/search?q=")
                preferenceManager.setSelectedSearchEnginePosition(0)
            }
        }

        binding.cacheSize.text = preferenceManager.getChacheSize()

        val entry = DataUsageEntry(Date(), preferenceManager.getCurrenDataUsage().toString())
        dataUsageList.add(entry)

        println(preferenceManager.getCurrenDataUsage().toString())
        val totalDataUsage = dataUsageList.sumBy { it.dataUsage.toInt()}
        val formattedDataUsage = formatFileSize(totalDataUsage.toLong())
        binding.internetUsage.text = formattedDataUsage
    }

    private fun formatFileSize(bytes: Long): String {
        val decimalFormat = DecimalFormat("0.00")
        val kiloBytes = bytes / 1024.0
        val megaBytes = kiloBytes / 1024.0
        val gigaBytes = megaBytes / 1024.0
        val teraBytes = gigaBytes / 1024.0

        return when {
            teraBytes >= 1 -> "${decimalFormat.format(teraBytes)} TB"
            gigaBytes >= 1 -> "${decimalFormat.format(gigaBytes)} GB"
            megaBytes >= 1 -> "${decimalFormat.format(megaBytes)} MB"
            kiloBytes >= 1 -> "${decimalFormat.format(kiloBytes)} KB"
            else -> "$bytes B"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}