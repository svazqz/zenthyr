# zenthyr

A Clojure-based desktop application framework that combines the power of Clojure with modern web technologies. It provides a seamless integration between a Clojure backend and an Electron-powered frontend using React.

## Features

- **Clojure Backend**: Robust server-side logic with Clojure's powerful data processing capabilities
- **Modern Frontend**: React-based UI with hot-reload support through Vite
- **IPC Communication**: Bidirectional communication between frontend and backend via WebSocket
- **Desktop Integration**: Native desktop application packaging using Electron

## Prerequisites

- Java JDK 8 or later
- Node.js and npm
- Clojure CLI tools

## Usage

To start the development environment:

```bash
lein dev
```

This will:

1. Install necessary Node.js dependencies
2. Start the Vite development server
3. Launch the WebSocket server for IPC
4. Open the Electron window with your application
## Development
The project structure:

- src/zenthyr/ : Core framework components
- src/app/ : React frontend application
- src/main.clj : Application logic implementation
## License

Copyright © 2025

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

### Third-Party Licenses

This project includes Electron, which is licensed under the MIT License.
Copyright (c) Electron contributors
Copyright (c) 2013-2020 GitHub Inc.

See the [Electron License](https://github.com/electron/electron/blob/main/LICENSE) for more details.

Note: When distributing this application, ensure compliance with Electron's license terms and include appropriate attributions in your distribution.

```plaintext

This updated README provides a clear overview of what zenthyr is, its main features, prerequisites, usage instructions, and development information while maintaining the original license information.
 ```
```