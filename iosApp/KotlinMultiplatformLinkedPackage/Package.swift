// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(
      path: "../../bottomsheet-spm"
    ),
    .package(path: "subpackages/_bottomsheet_mpp")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(
          name: "BottomSheetSPM",
          package: "bottomsheet-spm"
        ),
        .product(name: "_bottomsheet_mpp", package: "_bottomsheet_mpp")
      ]
    )
  ]
)
