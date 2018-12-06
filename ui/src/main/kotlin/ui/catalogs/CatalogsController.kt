/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.catalogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.support.v7.widget.RxToolbar
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.catalogs_controller.*
import tachiyomi.app.R
import tachiyomi.source.CatalogSource
import tachiyomi.ui.base.MvpController
import tachiyomi.ui.base.withFadeTransition
import tachiyomi.ui.catalogbrowse.CatalogBrowseController
import tachiyomi.ui.home.HomeChildController

class CatalogsController : MvpController<CatalogsPresenter>(),
  CatalogsAdapter.Listener,
  HomeChildController {

  private var adapter: CatalogsAdapter? = null

  override fun getPresenterClass() = CatalogsPresenter::class.java

  //===========================================================================
  // ~ Lifecycle
  //===========================================================================

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    return inflater.inflate(R.layout.catalogs_controller, container, false)
  }

  override fun onViewCreated(view: View) {
    super.onViewCreated(view)
    adapter = CatalogsAdapter(this)
    catalogs_recycler.layoutManager = LinearLayoutManager(view.context)
    catalogs_recycler.adapter = adapter

    // TODO no mapping, handle threading from presenter
    presenter.state
      .map { it.catalogs }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeWithView(::renderCatalogues)
  }

  override fun onDestroyView(view: View) {
    adapter = null
    super.onDestroyView(view)
  }

  //===========================================================================
  // ~ Render
  //===========================================================================

  private fun renderCatalogues(catalogs: List<CatalogSource>) {
    adapter?.submitList(catalogs)
  }

  //===========================================================================
  // ~ User interaction
  //===========================================================================

  override fun onRowClick(catalog: CatalogSource) {
    router.pushController(CatalogBrowseController(catalog.id).withFadeTransition())
  }

  override fun onBrowseClick(catalog: CatalogSource) {
    onRowClick(catalog)
  }

  override fun onLatestClick(catalog: CatalogSource) {
  }

}
