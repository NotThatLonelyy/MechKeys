# Changelog

## 1.0.0
First release of MechKeys in a single repository.
* Fabric 26.1 / 26.2 / 26.3 and Forge 1.8.9 from one repo, sharing one Minecraft-free core (`common/`).
* Separate keyboard and mouse sounds with their own profile, ON/OFF switch and volume.
* Imports Mechvibes V2 (`single` / `multi`) and V1 packs from a zip, a folder or a link.
* Two default packs (amazin keychron q3 bluex, X2 SuperStrike) and 11 optional packs in `soundpacks/`.
* One GitHub workflow builds every version; tags `v*` publish all jars to one release.
