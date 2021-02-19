import noSerial from './noSerial.png'
import './SerialNotActive.css'
function SerialNotActive() {
    return <div className="App">
        <header className="App-header">
            <div><img src={noSerial} alt="notactive" height="250px" width="250px" /></div>
            <div>Serial API is <b>not Active!</b></div>
            <hr/>
            <div>- Do you use Chrome version 80+?</div>
            <div>- Did you <b>enable experimental web platform features</b> in chrome flags?</div>
            <div>use: <i>chrome://flags/#enable-experimental-web-platform-features</i></div>
            <div>- Restart Chrome!</div>
        </header>
    </div>;
}
export default SerialNotActive;
