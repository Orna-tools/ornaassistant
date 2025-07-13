package com.lloir.ornaassistant.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.model.ItemType
import com.lloir.ornaassistant.domain.model.OrnaItem
import com.lloir.ornaassistant.presentation.viewmodel.ItemBrowserViewModel
import com.lloir.ornaassistant.presentation.viewmodel.ItemBrowserUiState
import com.lloir.ornaassistant.presentation.viewmodel.TierFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ItemBrowserFragment : Fragment() {

    private val viewModel: ItemBrowserViewModel by viewModels()
    private lateinit var itemsAdapter: ItemsAdapter
    
    // View references
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var typeChipGroup: ChipGroup
    private lateinit var tierChipGroup: ChipGroup
    private lateinit var bossChip: Chip
    private lateinit var clearFiltersButton: Button
    private lateinit var countTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var progressBar: CircularProgressIndicator
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_item_browser, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize view references
        recyclerView = view.findViewById(R.id.recyclerViewItems)
        searchEditText = view.findViewById(R.id.editTextSearch)
        typeChipGroup = view.findViewById(R.id.chipGroupTypes)
        tierChipGroup = view.findViewById(R.id.chipGroupTiers)
        bossChip = view.findViewById(R.id.chipBossOnly)
        clearFiltersButton = view.findViewById(R.id.buttonClearFilters)
        countTextView = view.findViewById(R.id.textViewCount)
        errorTextView = view.findViewById(R.id.textViewError)
        progressBar = view.findViewById(R.id.progressBar)
        
        setupRecyclerView()
        setupSearchField()
        setupFilterChips()
        setupBossFilter()
        setupClearFiltersButton()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        itemsAdapter = ItemsAdapter { item ->
            // Handle item click - could navigate to detail view
        }
        
        recyclerView.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSearchField() {
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setSearchQuery(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
        
        // Update search query as user types
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })
    }
    
    private fun setupFilterChips() {
        // Item type filter chips
        ItemType.values().forEach { itemType ->
            val chip = layoutInflater.inflate(
                R.layout.chip_choice, 
                typeChipGroup, 
                false
            ) as Chip
            
            chip.text = itemType.displayName
            chip.tag = itemType
            chip.setOnClickListener {
                val selectedType = if (chip.isChecked) itemType else null
                viewModel.setSelectedType(selectedType)
            }
            typeChipGroup.addView(chip)
        }
        
        // Tier filter chips
        val tiers = listOf("All", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10")
        tiers.forEachIndexed { index, tierName ->
            val chip = layoutInflater.inflate(
                R.layout.chip_choice, 
                tierChipGroup, 
                false
            ) as Chip
            
            chip.text = tierName
            chip.tag = index // 0 = All, 1-10 = Tier number
            chip.setOnClickListener {
                val tierFilter = when {
                    !chip.isChecked -> TierFilter.ALL
                    index == 0 -> TierFilter.ALL
                    else -> TierFilter.SINGLE(index)
                }
                viewModel.setTierFilter(tierFilter)
            }
            tierChipGroup.addView(chip)
        }
    }
    
    private fun setupBossFilter() {
        bossChip.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowBossOnly(isChecked)
        }
    }
    
    private fun setupClearFiltersButton() {
        clearFiltersButton.setOnClickListener {
            // Clear all filters
            searchEditText.setText("")
            typeChipGroup.clearCheck()
            tierChipGroup.check(tierChipGroup.getChildAt(0).id) // Select "All"
            bossChip.isChecked = false
            viewModel.clearFilters()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUI(state)
                    }
                }
                
                // Observe search query
                launch {
                    viewModel.searchQuery.collectLatest { query ->
                        if (searchEditText.text.toString() != query) {
                            searchEditText.setText(query)
                        }
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: ItemBrowserUiState) {
        // Update loading state
        progressBar.isVisible = state.isLoading
        
        // Update error state
        errorTextView.isVisible = state.error != null
        errorTextView.text = state.error
        
        // Update items count
        countTextView.text = if (state.items.isEmpty() && state.error == null && !state.isLoading) {
            getString(R.string.no_items_found)
        } else {
            getString(R.string.items_count, state.items.size)
        }
        
        // Update recycler view
        itemsAdapter.submitList(state.items)
    }
    
    /**
     * Adapter for the items RecyclerView
     */
    private inner class ItemsAdapter(
        private val onItemClick: (OrnaItem) -> Unit
    ) : ListAdapter<OrnaItem, ItemsAdapter.ItemViewHolder>(
        object : DiffUtil.ItemCallback<OrnaItem>() {
            override fun areItemsTheSame(oldItem: OrnaItem, newItem: OrnaItem): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: OrnaItem, newItem: OrnaItem): Boolean {
                return oldItem == newItem
            }
        }
    ) {
        
        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.textViewItemName)
            private val typeTextView: TextView = itemView.findViewById(R.id.textViewItemType)
            private val tierTextView: TextView = itemView.findViewById(R.id.textViewItemTier)
            private val statsTextView: TextView = itemView.findViewById(R.id.textViewStats)
            private val bossChip: Chip = itemView.findViewById(R.id.chipBossItem)
            
            fun bind(item: OrnaItem) {
                nameTextView.text = item.displayName
                typeTextView.text = item.typeDisplay
                tierTextView.text = item.tierDisplay
                
                // Show/hide boss chip
                bossChip.isVisible = item.isBossItem
                
                // Format stats
                val stats = item.stats
                val statsText = buildString {
                    if (stats.atk > 0) append("Atk: ${stats.atk} • ")
                    if (stats.mag > 0) append("Mag: ${stats.mag} • ")
                    if (stats.def > 0) append("Def: ${stats.def} • ")
                    if (stats.res > 0) append("Res: ${stats.res} • ")
                    if (stats.hp > 0) append("HP: ${stats.hp} • ")
                    if (stats.mana > 0) append("Mana: ${stats.mana} • ")
                    if (stats.dex > 0) append("Dex: ${stats.dex} • ")
                    if (stats.ward > 0) append("Ward: ${stats.ward}% • ")
                    if (stats.crit > 0) append("Crit: ${stats.crit}% • ")
                    
                    // Remove trailing separator if exists
                    if (endsWith(" • ")) {
                        setLength(length - 3)
                    }
                }
                
                statsTextView.text = statsText
                
                // Set click listener
                itemView.setOnClickListener { onItemClick(item) }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_item, parent, false)
            return ItemViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
}