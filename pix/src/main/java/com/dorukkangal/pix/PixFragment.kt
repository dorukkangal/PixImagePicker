package com.dorukkangal.pix

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.dorukkangal.pix.adapters.InstantImageAdapter
import com.dorukkangal.pix.adapters.MainImageAdapter
import com.dorukkangal.pix.databinding.ControlsLayoutBinding
import com.dorukkangal.pix.databinding.FragmentPixBinding
import com.dorukkangal.pix.databinding.GridLayoutBinding
import com.dorukkangal.pix.databinding.PermissionsLayoutBinding
import com.dorukkangal.pix.databinding.VideoCounterLayoutBinding
import com.dorukkangal.pix.helpers.CameraXManager
import com.dorukkangal.pix.helpers.LocalResourceManager
import com.dorukkangal.pix.helpers.PixBus
import com.dorukkangal.pix.helpers.PixEventCallback
import com.dorukkangal.pix.helpers.cancelAnimation
import com.dorukkangal.pix.helpers.color
import com.dorukkangal.pix.helpers.getScrollProportion
import com.dorukkangal.pix.helpers.handler
import com.dorukkangal.pix.helpers.hide
import com.dorukkangal.pix.helpers.hideBubble
import com.dorukkangal.pix.helpers.hideScrollbar
import com.dorukkangal.pix.helpers.hideStatusBar
import com.dorukkangal.pix.helpers.instantImageAdapter
import com.dorukkangal.pix.helpers.longSelectionStatus
import com.dorukkangal.pix.helpers.mBubbleAnimator
import com.dorukkangal.pix.helpers.mScrollbarAnimator
import com.dorukkangal.pix.helpers.mViewHeight
import com.dorukkangal.pix.helpers.mainImageAdapter
import com.dorukkangal.pix.helpers.permissionsFilter
import com.dorukkangal.pix.helpers.sScrollbarHideDelay
import com.dorukkangal.pix.helpers.scrollListener
import com.dorukkangal.pix.helpers.selection
import com.dorukkangal.pix.helpers.sendButtonStateAnimation
import com.dorukkangal.pix.helpers.setDrawableIconForFlash
import com.dorukkangal.pix.helpers.setRecyclerViewPosition
import com.dorukkangal.pix.helpers.setSelectionText
import com.dorukkangal.pix.helpers.setUpMargins
import com.dorukkangal.pix.helpers.setViewPositions
import com.dorukkangal.pix.helpers.setup
import com.dorukkangal.pix.helpers.setupClickControls
import com.dorukkangal.pix.helpers.setupMainRecyclerView
import com.dorukkangal.pix.helpers.setupScreen
import com.dorukkangal.pix.helpers.show
import com.dorukkangal.pix.helpers.showBubble
import com.dorukkangal.pix.helpers.showScrollbar
import com.dorukkangal.pix.helpers.toPx
import com.dorukkangal.pix.helpers.toast
import com.dorukkangal.pix.helpers.toolbarHeight
import com.dorukkangal.pix.interfaces.OnSelectionListener
import com.dorukkangal.pix.models.Img
import com.dorukkangal.pix.models.Options
import com.dorukkangal.pix.models.PixViewModel
import com.dorukkangal.pix.utility.ARG_PARAM_PIX
import com.dorukkangal.pix.utility.ARG_PARAM_PIX_KEY
import com.dorukkangal.pix.utility.CustomItemTouchListener
import com.dorukkangal.pix.utility.PixBindings
import com.dorukkangal.pix.utility.parcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Created By Akshay Sharma on 17,June,2021
 * https://ak1.io
 */
class PixFragment(private val resultCallback: ((PixEventCallback.Results) -> Unit)? = null) :
    Fragment(), View.OnTouchListener {

    private val model: PixViewModel by viewModels()
    private var _binding: PixBindings? = null
    private val binding get() = _binding!!

    private var permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all {
                    it.value
                }) {
                binding.permissionsLayout.permissionsLayout.hide()
                binding.gridLayout.gridLayout.show()
                initialise(requireActivity())
            } else {
                binding.gridLayout.gridLayout.hide()
                binding.permissionsLayout.permissionsLayout.show()
            }
        }

    internal val mScrollbarHider = Runnable { binding.hideScrollbar() }
    private var cameraXManager: CameraXManager? = null
    private lateinit var options: Options
    private var mBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var scope = CoroutineScope(Dispatchers.IO)
    private var colorPrimaryDark = 0

    override fun onResume() {
        super.onResume()
        if (mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    requireActivity().hideStatusBar()
                } catch (e: IllegalStateException) {
                    e.message?.let { Log.e("PixFragment", it) }
                }
            }, 200)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mBottomSheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        options = arguments?.parcelable(ARG_PARAM_PIX) ?: Options()
        requireActivity().let {
            it.setupScreen()
            it.actionBar?.hide()
            colorPrimaryDark = it.color(R.color.primary_color_pix)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = run {
        val fragmentPixBinding = FragmentPixBinding.inflate(inflater, container, false)
        _binding = PixBindings(
            fragmentPixBinding,
            VideoCounterLayoutBinding.bind(fragmentPixBinding.root),
            PermissionsLayoutBinding.bind(fragmentPixBinding.root),
            GridLayoutBinding.bind(fragmentPixBinding.root),
            ControlsLayoutBinding.bind(fragmentPixBinding.root)
        )
        fragmentPixBinding.root
    }


    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setup()
    }

    private fun FragmentActivity.setup() {
        setUpMargins(binding)
        permissions()
        reSetup(this)
        //in case of resetting the options in an live fragment
        setFragmentResultListener(ARG_PARAM_PIX_KEY) { _, bundle ->
            val options1: Options? = bundle.parcelable(ARG_PARAM_PIX)
            options1?.let {
                this@PixFragment.options.preSelectedUrls.apply {
                    clear()
                    addAll(it.preSelectedUrls)
                }
            }
            permReqLauncher.permissionsFilter(this, options) {

                retrieveMedia()
            }

        }
    }

    private fun permissions() {
        binding.permissionsLayout.permissionButton.setOnClickListener {
            permReqLauncher.permissionsFilter(requireActivity(), options) {
                initialise(requireActivity())
            }
        }
    }

    private fun reSetup(context: FragmentActivity) {
        permReqLauncher.permissionsFilter(context, options) {
            initialise(context)
        }
    }


    private fun initialise(context: FragmentActivity) {
        binding.permissionsLayout.permissionsLayout.hide()
        binding.gridLayout.gridLayout.show()
        cameraXManager = CameraXManager(binding.fragmentPix.viewFinder, context, options).also {
            it.startCamera()
        }
        setupAdapters(context)
        setupFastScroller(context)
        observeSelectionList()
        retrieveMedia()
        setBottomSheetBehavior()
        setupControls()
        backPressController()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.imageList.removeObservers(requireActivity())
        model.selectionList.removeObservers(requireActivity())
        model.longSelection.removeObservers(requireActivity())
        model.callResults.removeObservers(requireActivity())
    }

    private fun observeSelectionList() {
        model.setOptions(options)
        model.imageList.observe(requireActivity()) {
            //Log.e(TAG, "imageList size is now ${it.list.size}")
            instantImageAdapter.addImageList(it.list)
            mainImageAdapter.addImageList(it.list)
            model.selectionList.value?.addAll(it.selection)
            model.selectionList.postValue(model.selectionList.value)
            binding.gridLayout.arrowUp.apply {
                if (mainImageAdapter.listSize != 0) show() else hide()
            }
        }
        model.selectionList.observe(requireActivity()) {
            //Log.e(TAG, "selectionList size is now ${it.size}")
            if (it.size == 0) {
                model.longSelection.postValue(false)
            } else if (!model.longSelectionValue) {
                model.longSelection.postValue(true)
            }
            binding.setSelectionText(requireActivity(), it.size)
        }
        model.longSelection.observe(requireActivity()) {
            //Log.e(TAG, "longSelection is now changed to  $it")
            binding.longSelectionStatus(it)
            if ((mBottomSheetBehavior?.state
                    ?: BottomSheetBehavior.STATE_COLLAPSED) == BottomSheetBehavior.STATE_COLLAPSED
            ) {
                binding.gridLayout.sendButtonStateAnimation(it)
            }
        }
        model.callResults.observe(requireActivity()) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let { set ->
                model.selectionList.postValue(HashSet())
                options.preSelectedUrls.clear()
                val results = set.map { it.contentUrl }
                resultCallback?.invoke(PixEventCallback.Results(results))
                PixBus.returnObjects(
                    event = PixEventCallback.Results(
                        results,
                        PixEventCallback.Status.SUCCESS
                    )
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFastScroller(context: FragmentActivity) {
        toolbarHeight = context.toPx(56f)
        binding.gridLayout.apply {
            fastscrollScrollbar.hide()
            fastscrollBubble.hide()
            fastscrollHandle.setOnTouchListener(this@PixFragment)
        }
    }


    private fun backPressController() {
        CoroutineScope(Dispatchers.Main).launch {
            PixBus.on(this) {
                val list = model.selectionList.value ?: HashSet()
                when {
                    list.size > 0 -> {
                        for (img in list) {
                            //  options.preSelectedUrls = ArrayList()
                            instantImageAdapter.select(false, img.position)
                            mainImageAdapter.select(false, img.position)
                        }
                        model.selectionList.postValue(HashSet())
                    }
                    mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED -> {
                        mBottomSheetBehavior?.setState(BottomSheetBehavior.STATE_COLLAPSED)
                    }
                    else -> {
                        model.returnObjects()
                    }
                }
            }
        }
    }

    private fun setupControls() {
        binding.setupClickControls(
            model = model,
            cameraXManager = cameraXManager,
            options = options,
            onModeChange = {
                options.mode = it
                setupControls()
            }
        ) { int, uri ->
            when (int) {
                0 -> model.returnObjects()
                1 -> mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                2 -> model.longSelection.postValue(true)
                3 -> {
                    if (model.selectionList.value.isNullOrEmpty()) {
                        model.selectionList.value?.add(Img(contentUrl = uri))
                        scope.cancel(CancellationException("canceled intentionally"))
                        model.returnObjects()
                        return@setupClickControls
                    }
                    model.selectionList.value?.add(Img(contentUrl = uri))
                    Handler(Looper.getMainLooper()).post {
                        binding.setSelectionText(
                            requireActivity(),
                            (model.selectionList.value ?: HashSet()).size
                        )
                        options.preSelectedUrls.clear()
                        options.preSelectedUrls.addAll(
                            (model.selectionList.value ?: HashSet()).map { it.contentUrl })
                        retrieveMedia()
                    }
                }
                4 -> if (model.longSelectionValue) binding.gridLayout.sendButtonStateAnimation(false)
                5 -> if (model.longSelectionValue) binding.gridLayout.sendButtonStateAnimation(true)

            }
        }
    }

    private fun retrieveMedia() {
        // options.preSelectedUrls.addAll(selectionList)
        if (options.preSelectedUrls.size > options.count) {
            val large = options.preSelectedUrls.size - 1
            val small = options.count
            for (i in large downTo small) {
                options.preSelectedUrls.removeAt(i)
            }
        }
        if (scope.isActive) {
            scope.cancel()
        }
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val localResourceManager = LocalResourceManager(requireContext()).apply {
                this.preSelectedUrls = options.preSelectedUrls
            }
            instantImageAdapter.clearList()
            mainImageAdapter.clearList()
            model.retrieveImages(localResourceManager)
        }

    }

    private fun setupAdapters(context: FragmentActivity) {
        val onSelectionListener: OnSelectionListener = object : OnSelectionListener {
            override fun onClick(element: Img?, view: View?, position: Int) {
                model.onImageSelected(element, position) {
                    val size = model.selectionListSize
                    if (options.count <= size) {
                        requireActivity().toast(size)
                        return@onImageSelected false
                    }
                    position.selection(it)
                    return@onImageSelected true
                }
            }

            override fun onLongClick(element: Img?, view: View?, position: Int) =
                model.onImageLongSelected(element, position) {
                    val size = model.selectionListSize
                    if (options.count <= size) {
                        requireActivity().toast(size)
                        return@onImageLongSelected false
                    }
                    position.selection(it)
                    return@onImageLongSelected true
                }
        }
        instantImageAdapter = InstantImageAdapter(context).apply {
            addOnSelectionListener(onSelectionListener)
        }
        mainImageAdapter = MainImageAdapter(context, options.spanCount).apply {
            addOnSelectionListener(onSelectionListener)
            setHasStableIds(true)
        }

        binding.gridLayout.apply {
            instantRecyclerView.adapter = instantImageAdapter
            instantRecyclerView.addOnItemTouchListener(CustomItemTouchListener(binding))
            recyclerView.setupMainRecyclerView(
                context, mainImageAdapter, scrollListener(this@PixFragment, binding)
            )
        }
    }


    private fun setBottomSheetBehavior() {
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.gridLayout.bottomSheet)
        requireActivity().setup(binding, mBottomSheetBehavior) {
            if (it) {
                showScrollbar(binding.gridLayout.fastscrollScrollbar, requireContext())
                mainImageAdapter.notifyDataSetChanged()
                mViewHeight = binding.gridLayout.fastscrollScrollbar.measuredHeight.toFloat()
                handler.post { binding.setViewPositions(getScrollProportion(binding.gridLayout.recyclerView)) }
                binding.gridLayout.sendButtonStateAnimation(show = false, withAnim = false)
            } else {
                instantImageAdapter.notifyDataSetChanged()
                binding.gridLayout.fastscrollScrollbar.hide()
                binding.gridLayout.sendButtonStateAnimation(model.longSelectionValue)
            }
        }
    }


    private fun CameraXManager.startCamera() {
        setUpCamera(binding)
        binding.controlsLayout.flashButton.show()
        binding.setDrawableIconForFlash(options)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                binding.apply {
                    if (event.x < gridLayout.fastscrollHandle.x - gridLayout.fastscrollHandle.paddingStart) {
                        return false
                    }
                    gridLayout.fastscrollHandle.isSelected = true
                    handler.removeCallbacks(mScrollbarHider)
                    cancelAnimation(mScrollbarAnimator, mBubbleAnimator)
                    if (!gridLayout.fastscrollScrollbar.isVisible && (gridLayout.recyclerView.computeVerticalScrollRange()
                                - mViewHeight > 0)
                    ) {
                        mScrollbarAnimator =
                            showScrollbar(gridLayout.fastscrollScrollbar, requireActivity())
                    }
                    showBubble()
                    val y = event.rawY
                    setViewPositions(y - toolbarHeight)
                    setRecyclerViewPosition(y)
                    v?.parent?.requestDisallowInterceptTouchEvent(true)
                }

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.rawY
                binding.setViewPositions(y - toolbarHeight)
                binding.setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v?.parent?.requestDisallowInterceptTouchEvent(false)
                binding.gridLayout.fastscrollHandle.isSelected = false
                handler.postDelayed(mScrollbarHider, sScrollbarHideDelay.toLong())
                binding.hideBubble()
                return true
            }
        }
        return false
    }
}
