package kamp.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class KotlinMPPLibrary(
  override val group: String,
  override val name: String,
  override val latestVersion: String,
  override val releaseVersion: String?,
  override val versions: List<String>?,
  override val lastUpdated: Long?,
  val targets: Set<KotlinTarget>,
  val description: String?,
  val website: String?,
  val scm: String?,
) : MavenArtifact {
  public constructor(
    artifact: MavenArtifact,
    targets: Set<KotlinTarget>,
    description: String?,
    website: String?,
    scm: String?,
  ) : this(
    group = artifact.group,
    name = artifact.name,
    latestVersion = artifact.latestVersion,
    releaseVersion = artifact.releaseVersion,
    versions = artifact.versions,
    lastUpdated = artifact.lastUpdated,
    targets = targets,
    description = description,
    website = website,
    scm = scm
  )

  val _id: String

  @Transient
  val isMultiplatform: Boolean = targets.size > 1

  @Transient
  val version: String = releaseVersion ?: latestVersion

  init {
    _id = "$group:$name"
  }
}
