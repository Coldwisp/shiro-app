package com.example.fastani.ui.result

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.example.fastani.FastAniApi
import com.example.fastani.MainActivity
import com.example.fastani.R
import com.example.fastani.toPx
import com.example.fastani.ui.GlideApp
import kotlinx.android.synthetic.main.episode_result.view.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.android.synthetic.main.home_card.view.*
import kotlinx.android.synthetic.main.home_card.view.imageView
import kotlinx.android.synthetic.main.search_result.view.*

const val DESCRIPTION_LENGTH = 200

class ResultFragment(data: FastAniApi.Card) : Fragment() {
    var data: FastAniApi.Card = data
    private val isMovie: Boolean = data.episodes == 1 && data.status == "FINISHED"
    private lateinit var resultViewModel: ResultViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        resultViewModel =
            activity?.let { ViewModelProviders.of(it).get(ResultViewModel::class.java) }!!
        return inflater.inflate(R.layout.fragment_results, container, false)
    }

    private fun loadSeason(index: Int) {
        title_season_cards.removeAllViews()
        var epNum = 0
        data.cdnData.seasons[index].episodes.forEach { fullEpisode ->
            val epIndex = epNum
            epNum++

            val card: View = layoutInflater.inflate(R.layout.episode_result, null)
            if (fullEpisode.thumb != null) {
                // Can be "N/A"
                if (fullEpisode.thumb.startsWith("http")) {
                    val glideUrl = GlideUrl(fullEpisode.thumb)
                    context?.let {
                        Glide.with(it)
                            .load(glideUrl)
                            .into(card.imageView)
                    }
                }
            }

            card.imageView.setOnClickListener {
                MainActivity.loadPlayer(epIndex, index, data)
            }

            var title = fullEpisode.title
            if (title == null || title.replace(" ", "") == "") {
                title = "Episode $epNum"
            }
            if (!isMovie) {
                title = "$epNum. $title"
            }
            card.cardTitle.text = title

            val pro = MainActivity.getViewPosDur(data.anilistId, index, epIndex)
            println("DURPOS:" + epNum + "||" + pro.pos + "|" + pro.dur)
            if (pro.dur > 0 && pro.pos > 0) {
                var progress : Int = (pro.pos * 100L / pro.dur).toInt()
                if(progress < 5) {
                    progress = 5
                }
                else if(progress > 95) {
                    progress = 100
                }
                card.video_progress.progress = progress
            } else {
                card.video_progress.alpha = 0f
            }

            title_season_cards.addView(card)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { _, _ -> return@setOnTouchListener true } // VERY IMPORTANT https://stackoverflow.com/questions/28818926/prevent-clicking-on-a-button-in-an-activity-while-showing-a-fragment

        title_holder.setPadding(
            title_holder.paddingLeft,
            MainActivity.statusHeight + title_background.minimumHeight - 44.toPx,
            title_holder.paddingRight,
            0,
        )

        val glideUrl =
            GlideUrl("https://fastani.net/" + data.bannerImage) { FastAniApi.currentHeaders }

        if (data.trailer != null) {
            title_background.setOnLongClickListener {
                Toast.makeText(context, data.title.english + " - Trailer", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }

            title_background.setOnClickListener() {
                MainActivity.loadPlayer(data.title.english + " - Trailer", "https://fastani.net/" + data.trailer!!)
            }
        } else {
            title_trailer_btt.alpha = 0f
        }

        // SEASON SELECTOR
        val seasonsTxt = data.cdnData.seasons.mapIndexed { i: Int, _: FastAniApi.Seasons -> "Season ${i + 1}" }
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, seasonsTxt)
        spinner.adapter = arrayAdapter
        class SpinnerClickListener : AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {}
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                loadSeason(p2)
            }
        }
        if (data.cdnData.seasons.size <= 1) {
            spinner.background = null
            spinner.isEnabled = false
        }
        spinner.onItemSelectedListener = SpinnerClickListener()
       // loadSeason(0)

        context?.let {
            GlideApp.with(it)
                .load(glideUrl)
                .into(title_background)
        }

        title_name.text = data.title.english
        var descript = data.description
        if (descript.length > DESCRIPTION_LENGTH) {
            descript = descript.substring(0, DESCRIPTION_LENGTH)
                .replace("<br>", "")
                .replace("<i>", "")
                .replace("</i>", "")
                .replace("\n", " ") + "..."
        }
        title_descript.text = descript
        title_duration.text = data.duration.toString() + "min"
        var ratTxt = (data.averageScore / 10f).toString().replace(',', '.') // JUST IN CASE DUE TO LANG SETTINGS
        if (!ratTxt.contains('.')) ratTxt += ".0"
        title_rating.text = "Rated: $ratTxt"
        title_genres.text = data.genres.joinToString(prefix = "", postfix = "", separator = "  ") //  •
    }
}