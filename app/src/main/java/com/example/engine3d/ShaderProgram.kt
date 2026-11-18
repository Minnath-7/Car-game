package com.example.engine3d

import android.opengl.GLES20

class ShaderProgram {
    val programId: Int

    val aPosLoc: Int
    val aNormalLoc: Int
    val aColorLoc: Int

    val uMvpLoc: Int
    val uModelLoc: Int
    val uLightDirLoc: Int
    val uCameraPosLoc: Int
    val uAmbientColorLoc: Int
    val uFogColorLoc: Int
    val uFogDensityLoc: Int
    val uEmissiveLoc: Int
    val uSpecStrengthLoc: Int
    val uSpecPowerLoc: Int
    val uColorTintLoc: Int

    init {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        aPosLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorLoc = GLES20.glGetAttribLocation(programId, "aColor")

        uMvpLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uLightDirLoc = GLES20.glGetUniformLocation(programId, "uLightDir")
        uCameraPosLoc = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uFogColorLoc = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLoc = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uEmissiveLoc = GLES20.glGetUniformLocation(programId, "uEmissive")
        uSpecStrengthLoc = GLES20.glGetUniformLocation(programId, "uSpecStrength")
        uSpecPowerLoc = GLES20.glGetUniformLocation(programId, "uSpecPower")
        uColorTintLoc = GLES20.glGetUniformLocation(programId, "uColorTint")
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec4 vColor;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
                vColor = aColor;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uLightDir;
            uniform vec3 uCameraPos;
            uniform vec3 uAmbientColor;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uEmissive;
            uniform float uSpecStrength;
            uniform float uSpecPower;
            uniform vec4 uColorTint;

            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec4 vColor;

            void main() {
                vec4 base = vColor * uColorTint;
                vec3 N = normalize(vWorldNormal);
                vec3 L = normalize(uLightDir);
                vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 H = normalize(L + V);

                float diff = max(dot(N, L), 0.0);
                float backFill = max(dot(N, -L), 0.0) * 0.22;
                float spec = pow(max(dot(N, H), 0.0), uSpecPower) * uSpecStrength;
                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0) * 0.28;

                vec3 litColor = base.rgb * (uAmbientColor + diff * 0.85 + backFill)
                              + vec3(1.0) * spec
                              + base.rgb * fresnel
                              + base.rgb * uEmissive;

                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = clamp(1.0 - exp(-dist * uFogDensity), 0.0, 0.92);
                vec3 finalRgb = mix(litColor, uFogColor, fogFactor);
                gl_FragColor = vec4(finalRgb, base.a);
            }
        """
    }
}
