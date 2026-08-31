package com.sunjk.sunjktool.data.sync

/**
 * Global sync trigger — avoids threading SyncEngine through every ViewModel factory.
 * Set by AppContainer after SyncEngine is created.
 */
object SyncTrigger {
    private var engine: SyncEngine? = null

    fun init(engine: SyncEngine) {
        this.engine = engine
    }

    fun requestAutoSync() {
        engine?.requestAutoSync()
    }

    /** Bump the mutation counter for the given entity, so the next sync
     *  detects the change (including deletions) even if max(updatedDate)
     *  hasn't changed. */
    fun bumpEntity(entity: String) {
        engine?.bumpEntityMutation(entity)
    }
}
