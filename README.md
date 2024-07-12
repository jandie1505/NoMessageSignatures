# NoMessageSignatures
## Overview
NoMessageSignatures is a minecraft plugin which removes the chat message signatures on packet level to disable chat reporting.
## How to install
To install this plugin, download the plugin for the correct minecraft version from the releases page and put it into your plugins directory.  
If the plugin is not available for your minecraft version, you can still use it, but it can only use the fallback mode where all chat messages will be sent as system messages and private messages are not protected.
## Commands
| Command | Description | Permission |
|--|--|--|
| `/nomessagesignatures` | Shows which types of messages are protected | Everyone |
| `/nomessagesignatures mode` | Shows the current mode that is used to prevent chat reporting | Everyone |
| `/nomessagesignatures reload` | Reload the config | Console |
## Configuration
The plugin should work out of the box without any configuration needed.  
But if you want to change something, here are the config values:
| Option | Default | Description |
|--|--|--|
| `disable_packet_mode` | `false` | Disables the Packet replacement mode. Changing this is not recommended. Read comment in config file for more information. |
| `hide_banner` | `false` | Hides the "Chat messages on this server can't be verified banner" (disable this if you encounter issues with it). |
| `announce_protections` | `true` | When enabled, every player will get a info message about which protections are enabled. |
