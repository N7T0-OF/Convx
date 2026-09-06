package com.convx.modulehost

/**
 * Declarative module host.
 *
 * Without a [ModuleStore] the host is in-memory only: registry state is lost on
 * restart. With a store, every committed transition is persisted and package bytes
 * are staged/committed atomically under the store root.
 */
class DeclarativeModuleHost(
    val convxVersion: String,
    private val store: ModuleStore? = null,
) {
    private val validator = DeclarativeModuleValidator(convxVersion)
    private val registry = DeclarativeModuleRegistry(onChange = { modules -> store?.saveRegistry(modules) })
    private var lifecycle = DeclarativeModuleHostLifecycle.NOT_STARTED

    init {
        if (store != null) {
            registry.restore(store.loadRegistry())
        }
    }

    @Synchronized
    fun start() {
        check(lifecycle == DeclarativeModuleHostLifecycle.NOT_STARTED) { "declarative module host is already started" }
        lifecycle = DeclarativeModuleHostLifecycle.STARTED
    }

    @Synchronized
    fun snapshot(): DeclarativeModuleHostSnapshot = DeclarativeModuleHostSnapshot(
        lifecycle = lifecycle,
        convxVersion = convxVersion,
        modules = registry.snapshot(),
    )

    fun validate(packageBytes: ByteArray): ValidatedModulePackage = validator.validate(packageBytes)

    @Synchronized
    fun validateAndRegister(packageBytes: ByteArray): RegisteredModule {
        checkStarted()
        return registry.registerValidated(validate(packageBytes))
    }

    /**
     * Atomically install or upgrade a registered module.
     *
     * [expectedSha256], when given, is verified before staging. The package is
     * written to a staging slot, then committed; a failure restores the previous
     * package bytes and marks the module [ModuleState.ROLLED_BACK] (or FAILED when
     * there was no previous version).
     */
    @Synchronized
    fun install(moduleId: String, packageBytes: ByteArray, expectedSha256: String? = null): RegisteredModule {
        checkStarted()
        expectedSha256?.let { ModuleIntegrity.verify(packageBytes, it) }
        val validated = validate(packageBytes)
        check(validated.manifest.id == moduleId) { "package manifest id does not match $moduleId" }

        registry.stage(validated)
        return try {
            store?.stagePackage(moduleId, packageBytes)
            store?.commitPackage(moduleId)
            registry.commitInstall(moduleId)
        } catch (error: Exception) {
            runCatching { store?.rollbackPackage(moduleId) }
            registry.failInstall(moduleId)
            throw ModuleValidationException("install of $moduleId failed and was rolled back", error)
        }
    }

    /** Restore the pre-upgrade package bytes and mark the module rolled back. */
    @Synchronized
    fun rollback(moduleId: String): RegisteredModule {
        checkStarted()
        val previousBytes = store?.previousPackage(moduleId)
            ?: throw ModuleValidationException("module $moduleId has no previous version to roll back to")
        // Revalidate before restoring: never restore bytes that no longer validate.
        val previousManifest = validate(previousBytes).manifest
        val restored = registry.rollback(moduleId, previousManifest)
        store.rollbackPackage(moduleId)
        return restored
    }

    @Synchronized
    fun markInstalled(moduleId: String): RegisteredModule {
        checkStarted()
        return registry.markInstalled(moduleId)
    }

    @Synchronized
    fun enable(moduleId: String): RegisteredModule {
        checkStarted()
        return registry.enable(moduleId)
    }

    @Synchronized
    fun disable(moduleId: String): RegisteredModule {
        checkStarted()
        return registry.disable(moduleId)
    }

    @Synchronized
    fun registered(moduleId: String): RegisteredModule? = registry.get(moduleId)

    private fun checkStarted() {
        check(lifecycle == DeclarativeModuleHostLifecycle.STARTED) {
            "declarative module host has not been started"
        }
    }
}