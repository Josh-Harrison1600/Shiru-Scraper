
export const About = () => {
    return (
        <div className="flex flex-col items-center mt-16 text-gray-200 text-center font-roboto">

            <h1 className="text-3xl font-roboto">What is Shiru?</h1>
                <p className="mt-8">
                    Shiru is a web app that scraps manga from certain websites and uses the OpenAI API to
                    <br></br>
                    determine the JLPT level associated with said manga!
                </p>
            
            <h2 className="text-3xl font-roboto mt-10">What is JLPT?</h2>
            <p className="mt-8">
                    The Japanese Language Proficiency Test (JLPT) is an exam issued by the Japan Foundation 
                    <br></br>
                    for learners of the language to assess their skills with vocabulary, grammar, listening 
                    <br></br>
                    and reading. The exams range from 5 levels, N5 - N1. N5 being the easiest and N1 being the hardest. 
                    <br></br>
                    I used this as my method of classifying difficulty as most Japanese learners are familiar with 
                    <br></br>
                    JLPT exams and where their skill level is in relation to them.
                </p>

            <h3 className="text-3xl font-roboto mt-10">Is Shiru Accurate?</h3>
            <p className="mt-8">
                Kinda...
                <br></br>
                This site uses the OpenAI API (ChatGPT) in order to determine the difficulty of these manga books so the 
                <br></br>
                placement of these manga in their respected categories should be taken with a grain of salt as ChatGPT
                <br></br>
                is known to hallucinate occasionally. However if a manga is placed incorrectly it shouldn't be more then
                <br></br>
                1 level. For example you shouldn't see any N2 manga in the N5 Category, but you might see some N3 manga 
                <br></br>
                in the N4 or N2 category occasionally for example.
            
            </p>
        </div>
    );
}

export default About;