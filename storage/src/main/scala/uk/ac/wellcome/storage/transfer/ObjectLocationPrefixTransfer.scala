package uk.ac.wellcome.storage.transfer

import uk.ac.wellcome.storage.{Location, ObjectLocation, ObjectLocationPrefix, Prefix}

trait ObjectLocationPrefixTransfer[SrcPrefix <: Prefix[_], DstLocation <: Location]
    extends PrefixTransfer[ObjectLocationPrefix, ObjectLocation] {
  override protected def buildDstLocation(
    srcPrefix: ObjectLocationPrefix,
    dstPrefix: ObjectLocationPrefix,
    srcLocation: ObjectLocation): ObjectLocation =
    dstPrefix.asLocation(
      srcLocation.path.stripPrefix(srcPrefix.path)
    )
}
