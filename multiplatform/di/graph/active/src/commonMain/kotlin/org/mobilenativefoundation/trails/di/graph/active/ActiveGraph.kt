package org.mobilenativefoundation.trails.di.graph.active

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.*
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.filters.*
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope
import org.mobilenativefoundation.trails.foundation.scope.LoggedInScope
import org.mobilenativefoundation.trails.model.domain.user.ActiveUser
import org.mobilenativefoundation.trails.screen.explore.*
import org.mobilenativefoundation.trails.screen.traildetail.*
import org.mobilenativefoundation.trails.screen.saved.*
import org.mobilenativefoundation.trails.screen.collection.*
import org.mobilenativefoundation.trails.screen.navigate.*
import org.mobilenativefoundation.trails.screen.activity.*
import org.mobilenativefoundation.trails.screen.foryou.*

@GraphExtension(scope = ActiveScope::class)
interface ActiveGraph {
    @Named("ActiveCircuit") val circuit: Circuit
    val user: ActiveUser.Composite
    val navigation: M1NavigationController
    val filters: FiltersFeature
    val saves: SaveTrailFeature
    val account: TrailAccount

    @Provides fun savedRepository(account: TrailAccount): SavedRepository = account.saved
    @Provides fun activityRepository(account: TrailAccount): ActivityRepository = account.activities
    @Provides fun forYouRepository(account: TrailAccount): ForYouRepository = account.forYou
    @Provides fun navigation(controller: M1NavigationController): M1Navigation = controller
    @Provides @SingleIn(ActiveScope::class)
    fun filters(repository: TrailRepository): FiltersFeature = RealFiltersFeature(repository)
    @Provides @SingleIn(ActiveScope::class)
    fun saves(repository: SavedRepository): SaveTrailFeature = RealSaveTrailFeature(repository)

    @Named("ActiveCircuit") @Provides @SingleIn(ActiveScope::class)
    fun provideCircuit(
        trails: TrailRepository,
        activities: ActivityRepository,
        forYou: ForYouRepository,
        saved: SavedRepository,
        navigation: M1Navigation,
        filters: FiltersFeature,
        saves: SaveTrailFeature,
        exploreUi: ExploreUi,
        detailUi: TrailDetailUi,
        savedUi: SavedUi,
        collectionUi: CollectionUi,
        navigateUi: NavigateUi,
        activityUi: ActivityUi,
        forYouUi: ForYouUi,
    ): Circuit = Circuit.Builder().apply {
        addUi<ForYouScreen, ForYouState> { state, modifier -> forYouUi.Content(state, modifier) }
        addPresenter<ForYouScreen, ForYouState> { _, _, _ -> ForYouPresenter(forYou, trails, saved, navigation, saves) }
        addUi<ActivityScreen, ActivityState> { state, modifier -> activityUi.Content(state, modifier) }
        addPresenter<ActivityScreen, ActivityState> { _, _, _ -> ActivityPresenter(activities, trails, saved, navigation, saves) }
        addUi<NavigateScreen, NavigateState> { state, modifier -> navigateUi.Content(state, modifier) }
        addPresenter<NavigateScreen, NavigateState> { _, _, _ -> NavigatePresenter(trails, saved, navigation) }
        addUi<ExploreScreen, ExploreState> { state, modifier -> exploreUi.Content(state, modifier) }
        addUi<TrailDetailScreen, TrailDetailState> { state, modifier -> detailUi.Content(state, modifier) }
        addUi<SavedScreen, SavedState> { state, modifier -> savedUi.Content(state, modifier) }
        addUi<CollectionScreen, CollectionState> { state, modifier -> collectionUi.Content(state, modifier) }
        addPresenter<ExploreScreen, ExploreState> { _, _, _ -> ExplorePresenter(trails, saved, navigation, filters, saves) }
        addPresenter<TrailDetailScreen, TrailDetailState> { screen, _, _ -> TrailDetailPresenter(screen, trails, saved, navigation, saves) }
        addPresenter<SavedScreen, SavedState> { _, _, _ -> SavedPresenter(saved, navigation, saves) }
        addPresenter<CollectionScreen, CollectionState> { screen, _, _ -> CollectionPresenter(screen, saved, navigation, saves) }
    }.build()

    @ContributesTo(LoggedInScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createActiveGraph(
            @SingleIn(ActiveScope::class) @Provides backstack: SaveableBackStack,
            @Provides navigator: Navigator,
            @Provides user: ActiveUser.Composite,
            @Provides account: TrailAccount,
            @Provides navigation: M1NavigationController,
        ): ActiveGraph
    }
}
