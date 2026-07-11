import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // A stray lockfile elsewhere on disk makes Turbopack misdetect the workspace
  // root — pin it explicitly to this app.
  turbopack: {
    root: path.resolve(__dirname),
  },
  // Minimal, self-contained build output for the Docker image.
  output: "standalone",
};

export default nextConfig;
