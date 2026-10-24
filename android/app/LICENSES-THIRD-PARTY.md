# Third-party licenses — CraftCaller

| Dependency | License | Purpose |
|---|---|---|
| `com.googlecode.libphonenumber:libphonenumber` | Apache License 2.0 (© Google) | Phone-number parsing/normalization (E.164) — used in `com.craftcaller.app.util.Ext.kt` (`Craft.normalizeNumber`) |

License texts are available from each project's repository:
- libphonenumber: https://github.com/googlei18n/libphonenumber/blob/master/LICENSE (Apache-2.0)

Bundled audio assets are original (see `LICENSES-AUDIO.md`, CC0).
The community backend runs on the project's own Firebase instance; no third-party
caller-ID or Truecaller-compatible service is used, and no Truecaller private
data is accessed or scraped. There is no universal caller identification: only
the sources described in-app (saved contacts, local cache, opt-in community
submissions, and the optional project-owned business directory) are consulted.
