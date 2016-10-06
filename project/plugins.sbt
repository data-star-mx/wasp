// TODO find whether these are needed
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)



/*
 * Packaging plugins (these evict some of the ones pulled in by Play's sbt-plugin)
 */
/*addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
*/

/*
 * Play Framework plugins
 */
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.8")