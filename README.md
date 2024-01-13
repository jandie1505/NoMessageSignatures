# No Message Signatures
## Overview
NoMessageSignatures is a minecraft plugin which removes the chat message signatures on packet level to disable chat reporting.
## How to install
To install this plugin, download the plugin for the correct minecraft version from the releases page and put it into your plugins directory.  
If the plugin is not available for your minecraft version, you can still use it, but it can only use the fallback mode where all chat messages will be sent as system messages and private messages are not protected.
## Commands
| Command | Description | Permission |
|--|--|--|
| `/nomessagesignatures` | Shows which types of messages are protected | Everyone |
| `/nomessagesignatures reload` | Reload the config | Console |
## Configuration
The plugin should work out of the box without any configuration needed.  
But if you want to change something, here are the config values:
| Option | Default | Description |
|--|--|--|
| `mode` | `auto` | Changes the way the plugin removes the signatures from the message. This should only be changed if you know what you're doing. Read comment in config file for more information. |
| `hide_banner` | `false` | Hides the "Chat messages on this server can't be verified banner". |
| `announce_protections` | `true` | When enabled, every player will get a info message about which protections are enabled. |
