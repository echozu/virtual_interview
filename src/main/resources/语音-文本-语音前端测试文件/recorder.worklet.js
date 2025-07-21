/**
 * AudioWorklet 处理器，运行在独立的后台线程中
 * 负责：降采样 -> 格式转换 -> 将PCM数据发回主线程
 */
class RecorderProcessor extends AudioWorkletProcessor {
    constructor(options) {
        super();
        this.sourceSampleRate = options.processorOptions.sourceSampleRate;
        this.targetSampleRate = 16000;
    }

    // 高质量降采样（线性插值）
    downsample(inputData) {
        if (this.sourceSampleRate === this.targetSampleRate) {
            return inputData;
        }
        const ratio = this.sourceSampleRate / this.targetSampleRate;
        const downsampledLength = Math.floor(inputData.length / ratio);
        const result = new Float32Array(downsampledLength);
        for (let i = 0; i < downsampledLength; i++) {
            const inputIndex = i * ratio;
            const index1 = Math.floor(inputIndex);
            const index2 = index1 + 1;
            const fraction = inputIndex - index1;

            if (index2 >= inputData.length) {
                result[i] = inputData[index1];
            } else {
                const value1 = inputData[index1];
                const value2 = inputData[index2];
                result[i] = value1 + (value2 - value1) * fraction;
            }
        }
        return result;
    }

    floatTo16BitPCM(input) {
        const output = new Int16Array(input.length);
        for (let i = 0; i < input.length; i++) {
            const s = Math.max(-1, Math.min(1, input[i]));
            output[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
        }
        return output;
    }

    process(inputs) {
        // inputs[0][0] 代表第一个输入的第一个声道的Float32Array数据
        const rawData = inputs[0][0];

        if (rawData && rawData.length > 0) {
            const downsampledData = this.downsample(rawData);
            const pcmData = this.floatTo16BitPCM(downsampledData);
            
            // 将处理好的PCM数据（的底层Buffer）发送回主线程
            // 第二个参数 [pcmData.buffer] 是一个可转移对象列表，实现零拷贝，性能更好
            this.port.postMessage(pcmData.buffer, [pcmData.buffer]);
        }
        
        // 返回true，让处理器保持激活状态
        return true;
    }
}

registerProcessor('recorder-processor', RecorderProcessor); 