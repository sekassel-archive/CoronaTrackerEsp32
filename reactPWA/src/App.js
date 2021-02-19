import React from 'react';
//import './App.css';
import SerialIsActive from './pages/SerialIsActive'
import SerialNotActive from './pages/SerialNotActive'

function App() {
  if ('serial' in navigator) {
    return <SerialIsActive/>
  } else return <SerialNotActive/>
}

export default App;
