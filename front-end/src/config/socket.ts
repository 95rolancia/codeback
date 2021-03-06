import io from 'socket.io-client';

export default class SocketClient {
  io: any;

  constructor(private readonly baseURL: string) {
    this.io = io(this.baseURL);

    this.io.on('connect_error', (error: Error) => {
      throw new Error(`socket error ${error.message}`);
    });
  }

  join(id: string, nickname: string) {
    this.io.emit('join', { roomId: id, nickname }, (error: Error) => {
      console.log('join error', error.message);
    });
  }

  getSocket() {
    return this.io;
  }

  close() {
    this.io.disconnect();
  }

  compile(source: string, inputData: string, language: string) {
    let realLanguage;
    switch (language) {
      case 'C++':
        realLanguage = 'cpp';
        break;
      case 'Java':
        realLanguage = 'java';
        break;
      case 'JavaScript':
        realLanguage = 'nodejs';
        break;
      case 'Python':
        realLanguage = 'python3';
        break;
      default:
        throw new Error(`unknown language ${language}`);
    }

    this.io.emit(
      'compile',
      {
        source,
        input_data: inputData,
        language: realLanguage
      },
      (error: Error) => {
        console.log('compile send error', error.message);
      }
    );
  }
}
