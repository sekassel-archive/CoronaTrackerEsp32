
export class SlipFrame {
    constructor() {
      this.escaped = false;
      this.startSetted = false;
      this.endSetted = false;
      this.direction = -1;
      this.command = -1;
      this.size = new Uint8Array();
      this.value = new Uint8Array();
      this.data = null;
      this.dataPosition = 0;
    }
  
    insert(value) {
      switch (value) {
        case 0xc0:
          if (!this.startSetted) {
            this.startSetted = true;
            return;
          } else {
            this.endSetted = true;
          }
          break;
        case 0xdb:
          this.escaped = true;
          break;
        default:
  
          if (this.startSetted && !this.endSetted) {
            var value2 = value;
            if (this.escaped) {
              if (value == 0xdc) {
                value2 = 0xc0;
              } else if (value == 0xdd) {
                value2 = 0xdb;
              }
              this.escaped = false;
            }
  
            if (this.direction == -1) {
              this.direction = value2;
              return;
            }
            if (this.command == -1) {
              this.command = value2;
              return;
            }
  
            if (this.size.length < 1) {
              this.size = Uint8Array.of(value2)
              return;
            }
            if (this.size.length < 2) {
              this.size = Uint8Array.of(this.size[0], value2)
              return;
            }
  
  
            if (this.value.length < 1) {
              this.value = Uint8Array.of(value2)
              return;
            }
            if (this.value.length < 2) {
              this.value = Uint8Array.of(this.value[0], value2)
              return;
            }
            if (this.value.length < 3) {
              this.value = Uint8Array.of(this.value[0], this.value[1], value2)
              return;
            }
            if (this.value.length < 4) {
              this.value = Uint8Array.of(this.value[0], this.value[1], this.value[2], value2)
              return;
            }
  
            if (this.data == null) {
              this.data = new Uint8Array(this.size[0] + 16 * this.size[1])
            }
  
            this.data[this.dataPosition] = value2;
            this.dataPosition = this.dataPosition + 1;
          }
  
          break;
  
      }
    }
  }
